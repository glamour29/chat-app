package com.example.client.models

import com.google.gson.annotations.SerializedName

// --- 1. NHÓM REQUEST (Gửi lên Server) ---

// Gửi khi Đăng nhập
data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String
)

// Gửi khi Đăng ký
data class RegisterRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("fullName")
    val fullName: String,

    @SerializedName("phoneNumber")
    val phoneNumber: String
)

// Gửi khi Cập nhật thông tin (Đổi tên)
data class UpdateProfileRequest(
    @SerializedName("userId")
    val userId: String, // Server lấy ID từ Token, nhưng gửi kèm cũng không sao

    @SerializedName("fullName")
    val fullName: String
)

// --- 2. NHÓM RESPONSE (Nhận từ Server) ---

// Dữ liệu User chuẩn (Dùng chung cho nhiều response)
data class UserData(
    @SerializedName("_id") // Map trường "_id" của MongoDB vào biến "id" của Kotlin
    val id: String?,

    @SerializedName("username")
    val username: String?,

    @SerializedName("fullName")
    val fullName: String?,

    @SerializedName("phoneNumber")
    val phoneNumber: String?,

    @SerializedName("avatarUrl")
    val avatarUrl: String?
)

// Trả về khi Đăng nhập thành công
data class LoginResponse(
    @SerializedName("message")
    val message: String?,

    @SerializedName("token")
    val token: String?,

    @SerializedName("user")
    val user: UserData?
)

// Trả về khi Cập nhật Profile thành công
data class UserResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data") // Server trả về object user trong key "data"
    val data: UserData?
)

// Trả về khi Upload Avatar thành công
data class UploadAvatarResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("avatarUrl")
    val avatarUrl: String // Link ảnh mới nhất từ server
)