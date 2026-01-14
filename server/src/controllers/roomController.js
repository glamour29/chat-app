// src/controllers/roomController.js
const Room = require('../models/Room');

// 1. Lấy phòng chat với một người cụ thể (Tìm hoặc Tạo mới)
exports.getRoomWithUser = async (req, res) => {
    try {
        const myId = req.user.userId; // Lấy từ Token
        const { partnerId } = req.body; // Lấy ID đối phương từ Body

        // --- 📸 DEBUG LOG (Kiểm tra dữ liệu đầu vào) ---
        console.log("-------------------------------");
        console.log("🔍 [API] Mở phòng chat:");
        console.log("👉 User ID (Tôi):", myId);
        console.log("👉 Partner ID:", partnerId);
        
        if (!partnerId) {
            console.log("❌ Lỗi: Thiếu partnerId trong Body");
            return res.status(400).json({ message: "Thiếu ID người cần chat (partnerId)!" });
        }

        // Tìm xem đã có phòng 1-1 nào chứa cả 2 người này chưa
        // Lưu ý: Phải tìm đúng tên trường là 'members' và 'isGroup: false'
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
                members: [myId, partnerId], // <--- QUAN TRỌNG: Dùng 'members'
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
exports.getMyRooms = async (req, res) => {
    try {
        const myId = req.user.userId;
        
        // ---> LOG DEBUG <---
        console.log("--- 📥 [API] Lấy danh sách Inbox ---");
        console.log("👤 User ID:", myId);
        
        // Tìm tất cả phòng mà tôi là thành viên (có id của tôi trong mảng members)
        const rooms = await Room.find({ members: myId })
            .sort({ lastMessageTime: -1 }) // Sắp xếp tin mới nhất lên đầu
            .populate('members', 'username fullName avatarUrl'); // Lấy chi tiết user

        console.log("📦 Số phòng tìm thấy:", rooms.length);
        // ---------------------------

        res.json(rooms);
    } catch (error) {
        console.error("❌ Lỗi:", error);
        res.status(500).json({ message: "Lỗi Server: " + error.message });
    }
};