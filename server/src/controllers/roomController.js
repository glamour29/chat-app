// src/controllers/roomController.js
const Room = require('../models/Room');

// 1. Lấy phòng chat với một người cụ thể (Tìm hoặc Tạo mới - Chat 1-1)
exports.getRoomWithUser = async (req, res) => {
    try {
        const myId = req.user.userId; // Lấy từ Token
        const { partnerId } = req.body; // Lấy ID đối phương từ Body

        // --- 📸 DEBUG LOG (Kiểm tra dữ liệu đầu vào) ---
        console.log("-------------------------------");
        console.log("🔍 [API] Mở phòng chat 1-1:");
        console.log("👉 User ID (Tôi):", myId);
        console.log("👉 Partner ID:", partnerId);
        
        if (!partnerId) {
            console.log("❌ Lỗi: Thiếu partnerId trong Body");
            return res.status(400).json({ message: "Thiếu ID người cần chat (partnerId)!" });
        }

        // Tìm xem đã có phòng 1-1 nào chứa cả 2 người này chưa
        let room = await Room.findOne({
            isGroup: false, 
            members: { $all: [myId, partnerId] }
        }).populate('members', 'username fullName avatarUrl');

        // Nếu chưa có -> Tạo phòng mới
        if (!room) {
            console.log("⚡ Chưa có phòng -> Đang tạo mới...");
            room = new Room({
                isGroup: false,
                name: "", // Chat 1-1 không cần tên
                members: [myId, partnerId],
                lastMessage: "Bắt đầu cuộc trò chuyện",
                lastMessageTime: new Date()
            });
            await room.save();
            
            // Populate lại thông tin để trả về cho Client hiển thị đẹp luôn
            room = await room.populate('members', 'username fullName avatarUrl');
            
            console.log("✅ Đã tạo phòng mới thành công:", room._id);
        } else {
            console.log("✅ Đã tìm thấy phòng cũ:", room._id);
        }

        console.log("-------------------------------");
        res.json(room);

    } catch (error) {
        console.error("❌ LỖI SERVER:", error);
        res.status(500).json({ message: "Lỗi Server: " + error.message });
    }
};

// 2. Lấy danh sách các phòng chat của tôi (Inbox)
// server/src/controllers/roomController.js
// server/src/controllers/roomController.js
exports.getMyRooms = async (req, res) => {
    try {
        const myId = req.user.userId;
        const rooms = await Room.find({ members: myId })
            .populate('members', 'username fullName avatarUrl') // Quan trọng nhất
            .sort({ lastMessageTime: -1 });
        res.json(rooms);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// 3. Tạo nhóm chat mới (Group Chat)
exports.createGroup = async (req, res) => {
    try {
        const myId = req.user.userId;
        const { name, members } = req.body; 
        // members là mảng các ID user khác được thêm vào: ["id_user_1", "id_user_2"]

        console.log("--- 👥 [API] Tạo Group Chat ---");
        console.log("Tên nhóm:", name);
        console.log("Thành viên thêm vào:", members);

        // Validate dữ liệu
        if (!name || !members || !Array.isArray(members) || members.length === 0) {
            return res.status(400).json({ message: "Tên nhóm và danh sách thành viên không hợp lệ!" });
        }

        // Tạo danh sách thành viên đầy đủ (bao gồm cả Admin là người tạo)
        const allMembers = [myId, ...members];

        // Tạo phòng mới
        const newGroup = new Room({
            name: name,
            isGroup: true,
            admin: myId, // Người tạo là Admin
            members: allMembers,
            lastMessage: "Nhóm vừa được tạo",
            lastMessageTime: new Date()
        });

        await newGroup.save();

        // Populate thông tin để trả về cho Frontend hiển thị ngay lập tức
        const fullGroup = await Room.findById(newGroup._id)
            .populate('members', 'username fullName avatarUrl');

        console.log(`✅ Đã tạo nhóm thành công: ${fullGroup._id}`);
        res.json(fullGroup);

    } catch (error) {
        console.error("❌ Lỗi tạo nhóm:", error);
        res.status(500).json({ message: "Lỗi Server: " + error.message });
    }
};