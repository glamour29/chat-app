// File này đã được chuyển sang thư mục models/AuthModels.kt để tránh trùng lặp.
// Vui lòng không định nghĩa lại class tại đây.

// 2. Dữ liệu gửi lên khi Đăng ký
data class RegisterRequest(
    val username: String,
    val password: String,
    val fullName: String
)

// 3. Dữ liệu Server trả về (QUAN TRỌNG: Chứa Token)
data class LoginResponse(
    val success: Boolean?,
    val message: String?,
    val token: String?,     // 👈 Đây là vé thông hành
    val userId: String?,    // 👈 ID của người dùng
    val username: String?,
    val user: UserData
)
data class UserData(
    val _id: String?,
    val username: String?,
    val fullName: String?,
    val avatarUrl: String?
)