package com.example.client.view.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.client.api.RetrofitClient
import com.example.client.api.SocketHandler
import com.example.client.models.UpdateProfileRequest
import com.example.client.models.UploadAvatarResponse
import com.example.client.models.UserResponse
import com.example.client.utils.ImageUtils
import com.example.client.view.theme.TealPrimary
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("ChatAppPrefs", Context.MODE_PRIVATE)

    // 1. Lấy Tên hiển thị
    var username by remember {
        mutableStateOf(sharedPref.getString("FULL_NAME", null) ?: sharedPref.getString("USERNAME", "Unknown User") ?: "User")
    }

    // 2. Lấy Avatar
    var avatarUrl by remember {
        mutableStateOf(sharedPref.getString("AVATAR", null))
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // --- BỘ CHỌN ẢNH & UPLOAD ---
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUrl = uri.toString() // Hiển thị tạm

            val userId = sharedPref.getString("USER_ID", "") ?: ""
            val token = sharedPref.getString("TOKEN", "") ?: ""

            val file = ImageUtils.getFileFromUri(context, uri)

            if (file != null && userId.isNotEmpty() && token.isNotEmpty()) {
                isLoading = true
                Toast.makeText(context, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show()

                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("avatar", file.name, requestFile)
                val userIdPart = userId.toRequestBody("text/plain".toMediaTypeOrNull())

                // Header chứa Token (Bearer ...)
                val authHeader = "Bearer $token"

                RetrofitClient.instance.uploadAvatar(authHeader, body, userIdPart)
                    .enqueue(object : Callback<UploadAvatarResponse> {
                        override fun onResponse(call: Call<UploadAvatarResponse>, response: Response<UploadAvatarResponse>) {
                            isLoading = false
                            if (response.isSuccessful && response.body()?.success == true) {
                                val newServerUrl = response.body()!!.avatarUrl
                                sharedPref.edit().putString("AVATAR", newServerUrl).apply()
                                avatarUrl = newServerUrl
                                Toast.makeText(context, "Cập nhật Avatar thành công!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Lỗi server: ${response.code()}", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<UploadAvatarResponse>, t: Throwable) {
                            isLoading = false
                            Toast.makeText(context, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            } else {
                if(token.isEmpty()) Toast.makeText(context, "Lỗi: Phiên đăng nhập hết hạn", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trang cá nhân", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // --- AVATAR ---
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.size(130.dp).clickable {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Box(
                        modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.LightGray).border(2.dp, TealPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarUrl.isNullOrEmpty()) {
                            AsyncImage(model = avatarUrl, contentDescription = "Avatar", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Text(text = if (username.isNotEmpty()) username.take(1).uppercase() else "?", fontSize = 45.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(modifier = Modifier.size(36.dp).offset(x = (-4).dp, y = (-4).dp), shape = CircleShape, color = Color.White, shadowElevation = 4.dp) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Xin chào,", fontSize = 18.sp, color = Color.Gray)
                Text(text = username, fontSize = 28.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(40.dp))

                // --- NÚT ĐỔI TÊN ---
                Button(
                    onClick = { tempName = username; showEditDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đổi tên hiển thị")
                }

                Spacer(modifier = Modifier.weight(1f))

                // --- NÚT ĐĂNG XUẤT ---
                Button(
                    onClick = {
                        try { SocketHandler.getSocket()?.disconnect() } catch (e: Exception) {}
                        with(sharedPref.edit()) { clear(); apply() }
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ĐĂNG XUẤT", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TealPrimary)
                }
            }
        }
    }

    // --- DIALOG ĐỔI TÊN (Đã sửa: Thêm Token vào Header) ---
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Cập nhật tên") },
            text = { OutlinedTextField(value = tempName, onValueChange = { tempName = it }, label = { Text("Tên hiển thị mới") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(onClick = {
                    if (tempName.isNotBlank()) {
                        val userId = sharedPref.getString("USER_ID", "") ?: ""
                        val token = sharedPref.getString("TOKEN", "") ?: ""
                        val authHeader = "Bearer $token"

                        if (token.isNotEmpty()) {
                            // Gọi API kèm Token
                            RetrofitClient.instance.updateProfile(authHeader, UpdateProfileRequest(userId, tempName))
                                .enqueue(object : Callback<UserResponse> {
                                    override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                                        if (response.isSuccessful && response.body()?.success == true) {
                                            username = tempName
                                            sharedPref.edit().putString("FULL_NAME", tempName).apply()
                                            Toast.makeText(context, "Đổi tên thành công!", Toast.LENGTH_SHORT).show()
                                            showEditDialog = false
                                        } else {
                                            val errorMsg = response.errorBody()?.string() ?: response.message()
                                            Toast.makeText(context, "Lỗi server: $errorMsg", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                                        Toast.makeText(context, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        } else {
                            Toast.makeText(context, "Lỗi: Phiên đăng nhập hết hạn", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)) { Text("Lưu") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Hủy") } }
        )
    }
}