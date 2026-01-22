const User = require('../models/User');
const UserRepository = require('../repositories/UserRepository');

// 1. Tìm kiếm người dùng (GET /api/users?search=...)
exports.getUsers = async (req, res) => {
    try {
        const searchTerm = req.query.search;
        const currentUserId = req.user.userId || req.user.id;

        if (!searchTerm) return res.status(200).json([]);

        const users = await UserRepository.searchUsers(searchTerm);
        
        // Lọc bỏ chính mình khỏi kết quả
        const filteredUsers = users.filter(user => user._id.toString() !== currentUserId.toString());

        return res.status(200).json(filteredUsers);
    } catch (error) {
        console.error("Lỗi tìm kiếm:", error);
        return res.status(500).json({ success: false, message: "Lỗi Server" });
    }
};

// 2. Cập nhật Profile (PUT /api/users/update)
exports.updateProfile = async (req, res) => {
    try {
        const userId = req.user.userId || req.user.id;
        const { fullName, avatar } = req.body;

        const updatedUser = await User.findByIdAndUpdate(
            userId,
            { $set: { fullName: fullName, avatarUrl: avatar } },
            { new: true }
        ).select('-password');

        if (!updatedUser) return res.status(404).json({ success: false, message: "Không tìm thấy User" });

        return res.status(200).json({ success: true, message: "Cập nhật thành công!", user: updatedUser });
    } catch (error) {
        return res.status(500).json({ success: false, message: "Lỗi Server" });
    }
};

// 3. Lấy danh sách bạn bè (GET /api/users/friends)
exports.getFriends = async (req, res) => {
    try {
        const userId = req.user.userId || req.user.id;
        const user = await User.findById(userId).populate('friends', 'username fullName avatarUrl phoneNumber isOnline');
        return res.status(200).json(user.friends || []);
    } catch (error) {
        return res.status(500).json({ message: "Lỗi Server" });
    }
};

// 4. Lấy lời mời kết bạn đang chờ (GET /api/users/friends/pending)
exports.getPendingRequests = async (req, res) => {
    try {
        const userId = req.user.userId || req.user.id;
        const user = await User.findById(userId).populate('pendingRequests', 'username fullName avatarUrl phoneNumber');
        return res.status(200).json(user.pendingRequests || []);
    } catch (error) {
        return res.status(500).json({ message: "Lỗi Server" });
    }
};

// 5. Gửi lời mời kết bạn (POST /api/users/friends/request)
exports.sendFriendRequest = async (req, res) => {
    try {
        const { userId } = req.body; // ID người nhận
        const myId = req.user.userId || req.user.id;

        if (userId === myId) return res.status(400).json({ message: "Không thể kết bạn với chính mình" });

        // Thêm mình vào danh sách chờ của người kia
        await User.findByIdAndUpdate(userId, {
            $addToSet: { pendingRequests: myId }
        });

        // PHÁT TÍN HIỆU REAL-TIME QUA SOCKET
        const io = req.app.get('socketio');
        if (io) {
            io.to(userId).emit('new_friend_request', { from: myId });
            console.log(`==> Đã gửi thông báo socket tới user: ${userId}`);
        }

        return res.status(200).json({ success: true, message: "Đã gửi lời mời!" });
    } catch (error) {
        console.error("Lỗi gửi kết bạn:", error);
        return res.status(500).json({ message: "Lỗi Server" });
    }
};

// 6. Chấp nhận kết bạn (POST /api/users/friends/accept)
exports.acceptFriendRequest = async (req, res) => {
    try {
        const { userId } = req.body; // ID người gửi lời mời
        const myId = req.user.userId || req.user.id;

        // 1. Thêm nhau vào danh sách bạn bè của nhau
        await User.findByIdAndUpdate(myId, { 
            $addToSet: { friends: userId },
            $pull: { pendingRequests: userId } // Xóa khỏi danh sách chờ
        });
        await User.findByIdAndUpdate(userId, { 
            $addToSet: { friends: myId } 
        });

        return res.status(200).json({ success: true, message: "Đã trở thành bạn bè" });
    } catch (error) {
        return res.status(500).json({ message: "Lỗi Server" });
    }
};