package com.example.client.api

// 1. Import các thư viện mạng
import com.example.client.models.LoginRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// 2. Import đầy đủ các Model từ package models (Để không bị lỗi Type Mismatch)
import com.example.client.models.LoginResponse
import com.example.client.models.RegisterRequest
import com.example.client.models.UpdateProfileRequest
import com.example.client.models.UploadAvatarResponse
import com.example.client.models.UserResponse
import retrofit2.http.Header

interface AuthService {
    // 1. Đăng nhập
    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // 2. Đăng ký
    @POST("api/auth/register")
    fun register(@Body request: RegisterRequest): Call<LoginResponse>

    // --- CÁC HÀM MỚI (Đã trỏ đúng về models) ---

    // 3. Cập nhật tên (Profile)
    @POST("api/users/update")
    fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest): Call<UserResponse>

    // 4. Upload Avatar
    @Multipart
    @POST("api/users/upload-avatar")
    fun uploadAvatar(
        @Header("Authorization") token: String,
        @Part avatar: MultipartBody.Part,
        @Part("userId") userId: RequestBody
    ): Call<UploadAvatarResponse>
}

// ⚠️ QUAN TRỌNG: KHÔNG được viết thêm data class nào ở dưới này nữa
// vì chúng đã nằm bên file AuthModels rồi. Nếu viết lại sẽ bị lỗi "Argument type mismatch".