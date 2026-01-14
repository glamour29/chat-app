// src/middlewares/authHTTP.js
const jwt = require('jsonwebtoken');

module.exports = (req, res, next) => {
    const token = req.header('Authorization'); // Lấy token từ Header

    if (!token) return res.status(401).json({ message: "Không có quyền truy cập!" });

    try {
        // Token thường có dạng: "Bearer eyJhbG..." -> Cần cắt chữ Bearer nếu có
        const tokenString = token.replace("Bearer ", "");
        const decoded = jwt.verify(tokenString, process.env.JWT_SECRET);
        req.user = decoded; // Gắn thông tin user vào request
        next();
    } catch (err) {
        res.status(400).json({ message: "Token không hợp lệ!" });
    }
};