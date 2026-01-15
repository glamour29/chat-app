// src/routes/roomRoutes.js
const express = require('express');
const router = express.Router();
const roomController = require('../controllers/roomController');
const auth = require('../middlewares/authHTTP'); // Import middleware vừa tạo

// API: Tìm hoặc tạo phòng với người khác
// POST /api/rooms/open
router.post('/open', auth, roomController.getRoomWithUser);

// API: Lấy danh sách Inbox
// GET /api/rooms
router.get('/', auth, roomController.getMyRooms);
// API Tạo nhóm chat (POST /api/rooms/group)
router.post('/group', auth, roomController.createGroup);

module.exports = router;