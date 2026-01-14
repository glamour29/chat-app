// src/models/Room.js
const mongoose = require('mongoose');

const roomSchema = new mongoose.Schema({
    name: {
        type: String, 
        default: "" // Tên nhóm (nếu là chat 1-1 thì để rỗng cũng được)
    },
    isGroup: {
        type: Boolean,
        default: false // false: Chat 1-1, true: Chat nhóm
    },
    members: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User', // Liên kết sang bảng User
        required: true
    }],
    admin: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User' // Chỉ dùng khi isGroup = true
    },
    lastMessage: {
        type: String, 
        default: "Trò chuyện mới"
    },
    lastMessageTime: {
        type: Date,
        default: Date.now
    }
}, { timestamps: true }); // Tự động tạo createdAt và updatedAt

module.exports = mongoose.model('Room', roomSchema);