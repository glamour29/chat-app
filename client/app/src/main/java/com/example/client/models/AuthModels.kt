package com.example.client.models

import com.google.gson.annotations.SerializedName

// 1. Dữ liệu gửi lên khi Đăng nhập
data class LoginRequest(
    val username: String,
    val password: String
)

// 2. Dữ liệu gửi lên khi Đăng ký
data class RegisterRequest(
    val username: String,
    val password: String,
    val fullName: String,
    val phoneNumber: String
)

// Dữ liệu User lồng bên trong Response
data class UserData(
    @SerializedName("_id")
    val id: String?,
    val username: String?,
    val fullName: String?,
    val phoneNumber: String?,
    val avatarUrl: String?
)

// 3. Dữ liệu Server trả về (Đã sửa để khớp với authController.js)
data class LoginResponse(
    val message: String?,
    val token: String?,
    val user: UserData? // Server trả về object 'user' lồng bên trong
)
