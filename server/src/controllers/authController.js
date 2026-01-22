// src/controllers/authController.js
const User = require('../models/User');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

// 1. Hàm Đăng ký (Register)
exports.register = async (req, res) => {
    try {const { username, password, fullName, phoneNumber } = req.body;

        // Kiểm tra user tồn tại
        const existingUser = await User.findOne({ username });
        if (existingUser) {
            return res.status(400).json({ message: "Tài khoản đã tồn tại!" });
        }

        // Mã hóa mật khẩu
        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(password, salt);

        // Tạo User mới (có lưu phoneNumber)
        const newUser = new User({
            username,
            password: hashedPassword,
            fullName,
            phoneNumber: phoneNumber || ""
        });

        await newUser.save();

        res.status(201).json({ 
            message: "Đăng ký thành công!",
            user: {
                _id: newUser._id,
                username: newUser.username,
                fullName: newUser.fullName,
                phoneNumber: newUser.phoneNumber
            }
        });

    } catch (error) {
        console.error("Register Error:", error);
        res.status(500).json({ message: "Lỗi Server: " + error.message });
    }
};

// 2. Hàm Đăng nhập (Login)
exports.login = async (req, res) => {
    try {
        const { username, password } = req.body;

        // Tìm user
        const user = await User.findOne({ username });
        if (!user) {
            return res.status(400).json({ message: "Sai tên đăng nhập hoặc mật khẩu!" });
        }

        // Kiểm tra mật khẩu
        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) {
            return res.status(400).json({ message: "Sai tên đăng nhập hoặc mật khẩu!" });
        }

        // Tạo JWT Token
        const token = jwt.sign(
            { userId: user._id },
            process.env.JWT_SECRET || 'secret_key',
            { expiresIn: '30d' }
        );

        res.json({
            message: "Đăng nhập thành công!",
            token,
            user: {
                _id: user._id,
                username: user.username,
                fullName: user.fullName,
                avatarUrl: user.avatarUrl,
                phoneNumber: user.phoneNumber
            }
        });

    } catch (error) {
        console.error("Login Error:", error);
        res.status(500).json({ message: "Lỗi Server: " + error.message });
    }
};