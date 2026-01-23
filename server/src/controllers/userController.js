const User = require('../models/User'); // Import Model User
const ChatService = require('../services/ChatService'); // Import ChatService

// 1. Lấy danh sách users (trừ bản thân)
exports.getUsers = async (req, res) => {
    try {
        const users = await ChatService.getAllUsers();
        // Lọc bỏ chính mình
        const otherUsers = users.filter(u => u._id.toString() !== req.user.userId);
        res.json(otherUsers);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// 2. Cập nhật Profile (ĐÃ SỬA: Gọi trực tiếp DB để trả về đúng định dạng JSON)
exports.updateProfile = async (req, res) => {
    try {
        const userId = req.user.userId;
        const { fullName } = req.body;

        // Validate dữ liệu
        if (!fullName || fullName.trim().length === 0) {
            return res.status(400).json({ 
                success: false, 
                message: "Tên hiển thị không được để trống" 
            });
        }

        // Cập nhật vào MongoDB
        const updatedUser = await User.findByIdAndUpdate(
            userId,
            { fullName: fullName.trim() },
            { new: true } // Trả về data mới nhất sau khi update
        ).select('-password'); // Không trả về mật khẩu

        if (!updatedUser) {
            return res.status(404).json({ success: false, message: "Người dùng không tồn tại" });
        }

        // Trả về đúng cấu trúc Android mong đợi (UserResponse)
        res.json({
            success: true,
            message: "Cập nhật tên thành công",
            data: updatedUser
        });

    } catch (error) {
        console.error("Lỗi update profile:", error);
        res.status(500).json({ success: false, message: "Lỗi server: " + error.message });
    }
};

// 3. Upload Avatar
exports.uploadAvatar = async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ success: false, message: "Chưa chọn file ảnh" });
        }

        // Tạo đường dẫn ảnh đầy đủ
        const protocol = req.protocol;
        const host = req.get('host');
        // Lưu ý: Đảm bảo server.js đã có dòng: app.use('/uploads', express.static(...));
        const avatarUrl = `${protocol}://${host}/uploads/${req.file.filename}`;

        // Cập nhật URL vào Database
        const userId = req.user.userId;
        const updatedUser = await User.findByIdAndUpdate(
            userId, 
            { avatarUrl: avatarUrl }, 
            { new: true }
        ).select('-password');

        res.status(200).json({
            success: true,
            message: "Upload ảnh thành công",
            avatarUrl: avatarUrl,
            user: updatedUser
        });

    } catch (error) {
        console.error("Lỗi upload avatar:", error);
        res.status(500).json({ success: false, message: "Lỗi server khi upload ảnh" });
    }
};

// 4. Gửi lời mời kết bạn
exports.sendFriendRequest = async (req, res) => {
    try {
        const { userId } = req.body; // ID người nhận
        const myId = req.user.userId;

        if (userId === myId) return res.status(400).json({ message: "Không thể kết bạn với chính mình" });

        const targetUser = await User.findById(userId);
        if (!targetUser) return res.status(404).json({ message: "Người dùng không tồn tại" });

        const me = await User.findById(myId);
        if (me.friends.includes(userId)) return res.status(400).json({ message: "Hai người đã là bạn bè" });

        // A. Thêm vào pendingRequests
        await User.findByIdAndUpdate(userId, { $addToSet: { pendingRequests: myId } });

        // B. Tự động tạo/lấy phòng chat 1-1
        const room = await ChatService.createOrGetPrivateRoom(myId, userId);

        // C. Gửi Socket thông báo
        const io = req.app.get('socketio');
        if (io) {
            io.to(userId).emit('new_friend_request', { from: myId, roomId: room._id });
        }

        res.status(200).json({ success: true, message: "Đã gửi lời mời!", roomId: room._id });
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// 5. Chấp nhận kết bạn
exports.acceptFriendRequest = async (req, res) => {
    try {
        const { userId } = req.body; // ID người gửi lời mời
        const myId = req.user.userId;

        // A. Cập nhật danh sách bạn bè (2 chiều)
        await User.findByIdAndUpdate(myId, { 
            $addToSet: { friends: userId },
            $pull: { pendingRequests: userId } 
        });
        await User.findByIdAndUpdate(userId, { 
            $addToSet: { friends: myId } 
        });

        // B. Đảm bảo phòng Chat tồn tại
        const room = await ChatService.createOrGetPrivateRoom(myId, userId);

        res.status(200).json({ 
            success: true, 
            message: "Kết bạn thành công!",
            roomId: room._id 
        });
    } catch (error) {
        console.error("Lỗi chấp nhận kết bạn:", error);
        res.status(500).json({ message: error.message });
    }
};

// 6. Lấy danh sách lời mời kết bạn
exports.getPendingRequests = async (req, res) => {
    try {
        const user = await User.findById(req.user.userId)
            .populate('pendingRequests', 'username fullName avatarUrl phoneNumber');
        res.json(user ? user.pendingRequests : []);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// 7. Lấy danh sách bạn bè
exports.getFriends = async (req, res) => {
    try {
        const user = await User.findById(req.user.userId)
            .populate('friends', 'username fullName avatarUrl phoneNumber');
        res.json(user ? user.friends : []);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// 8. Tìm kiếm người dùng
exports.searchUsers = async (req, res) => {
    try {
        const { query } = req.query;
        const currentUserId = req.user.userId;

        if (!query || query.trim() === "") {
            return res.status(200).json([]);
        }

        const searchKey = query.trim();

        // A. Lấy danh sách bạn bè của mình để check trạng thái
        const me = await User.findById(currentUserId).select('friends');
        const myFriendIds = me.friends ? me.friends.map(id => id.toString()) : [];

        // B. Tìm kiếm
        const users = await User.find({
            $and: [
                { _id: { $ne: currentUserId } },
                {
                    $or: [
                        { phoneNumber: searchKey },
                        { username: { $regex: searchKey, $options: 'i' } }
                    ]
                }
            ]
        }).select('username fullName phoneNumber avatarUrl isOnline');

        // C. Gắn thêm trạng thái isFriend
        const results = users.map(user => {
            const userObj = user.toObject();
            userObj.isFriend = myFriendIds.includes(user._id.toString());
            return userObj;
        });

        res.status(200).json(results);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};