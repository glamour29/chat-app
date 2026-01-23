// server.js - FINAL VERSION (FIXED IMAGE & SOCKET)
const express = require('express');
const http = require('http');
const { Server } = require("socket.io");
const cors = require('cors');
const path = require('path'); // 1. 👇 THÊM DÒNG NÀY (Để xử lý đường dẫn file)

const connectDB = require('./src/config/db');
const authRoutes = require('./src/routes/authRoutes');
const messageRoutes = require('./src/routes/messageRoutes');
const roomRoutes = require('./src/routes/roomRoutes');
const userRoutes = require('./src/routes/userRoutes');
const socketAuthMiddleware = require('./src/middlewares/socketAuth');
const chatSocket = require('./src/sockets/chatSocket');
require('dotenv').config();

const app = express();
const server = http.createServer(app);

// 2. Kết nối Database
connectDB();

// 3. Cấu hình Middleware HTTP
app.use(cors());
app.use(express.json({ limit: '50mb' })); 
app.use(express.urlencoded({ limit: '50mb', extended: true }));

// 4. 👇 QUAN TRỌNG: Cấu hình để xem ảnh từ thư mục uploads
// Nếu không có dòng này, App Android sẽ không tải được ảnh
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// 5. Khai báo Routes API
app.use('/api/auth', authRoutes);
app.use('/api/messages', messageRoutes);
app.use('/api/rooms', roomRoutes);
app.use('/api/users', userRoutes);

// 6. Khởi tạo Socket.IO
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

// 7. 👇 QUAN TRỌNG: Lưu biến 'io' vào 'app'
// Để userController có thể dùng req.app.get('socketio') gửi thông báo
app.set('socketio', io);

// 8. Kích hoạt bảo vệ Socket (Middleware)
io.use(socketAuthMiddleware);

// 9. Lắng nghe sự kiện Socket
io.on("connection", (socket) => {
    console.log(`[Socket] User connected: ${socket.user.userId}`);
    // console.log(`[Socket] Socket ID: ${socket.id}`);

    // Tham gia vào room cá nhân (để nhận thông báo riêng tư)
    socket.join(socket.user.userId);

    // Kích hoạt tính năng Chat
    chatSocket(io, socket);

    socket.on("disconnect", () => {
        console.log(`[Socket] User disconnected: ${socket.user.userId}`);
    });
});

// 10. Chạy Server
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`-----------------------------------`);
    console.log(`[Server] Running at: http://localhost:${PORT}`);
    console.log(`[Images] Public at: http://localhost:${PORT}/uploads`); // Check link này
    console.log(`[API] Auth: http://localhost:${PORT}/api/auth/register`);
    console.log(`[Security] Socket Auth: ON`);
    console.log(`-----------------------------------`);
});