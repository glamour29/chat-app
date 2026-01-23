const express = require("express");
const router = express.Router();
const userController = require("../controllers/userController");
const authMiddleware = require("../middlewares/authHTTP");

// --- 1. CẤU HÌNH MULTER (ĐỂ UPLOAD ẢNH) ---
const multer = require("multer");
const path = require("path");

// Cấu hình nơi lưu ảnh và tên ảnh
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    // Đảm bảo bạn đã tạo thư mục 'uploads/' trong project server
    cb(null, 'uploads/'); 
  },
  filename: function (req, file, cb) {
    // Đặt tên file: avatar-timestamp.jpg
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, file.fieldname + '-' + uniqueSuffix + path.extname(file.originalname));
  }
});

const upload = multer({ storage: storage });
// ------------------------------------------

// 2. Route tìm kiếm
router.get("/", authMiddleware, userController.getUsers);
router.get("/search", authMiddleware, userController.searchUsers);

// 3. Route Update Profile (Khớp với @PUT bên Android)
router.put("/update", authMiddleware, userController.updateProfile);

// 4. 🔥 ROUTE MỚI: UPLOAD AVATAR (Khớp với @POST bên Android)
// Android gọi: api/users/upload-avatar
// 'avatar' là tên key mà bên Android gửi: MultipartBody.Part.createFormData("avatar", ...)
router.post("/upload-avatar", authMiddleware, upload.single('avatar'), userController.uploadAvatar);


// --- CÁC ROUTE BẠN BÈ (GIỮ NGUYÊN) ---
router.get('/friends', authMiddleware, userController.getFriends);
router.get('/friends/pending', authMiddleware, userController.getPendingRequests);
router.post('/friends/request', authMiddleware, userController.sendFriendRequest);
router.post('/friends/accept', authMiddleware, userController.acceptFriendRequest);

module.exports = router;