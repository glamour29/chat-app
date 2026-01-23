package com.example.client.view.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.client.api.RetrofitClient
import com.example.client.models.LoginRequest
import com.example.client.models.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    // 1. Khai báo biến trạng thái (State)
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // 2. Giao diện chính
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- HEADER: LOGO ---
        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = "App Logo",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome Back!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Đăng nhập để tiếp tục nhắn tin",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(40.dp))

        // --- INPUT: TÀI KHOẢN ---
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                isError = false
            },
            label = { Text("Tài khoản") },
            leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            isError = isError,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- INPUT: MẬT KHẨU ---
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                isError = false
            },
            label = { Text("Mật khẩu") },
            singleLine = true,
            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = "Toggle Password")
                }
            },
            shape = RoundedCornerShape(12.dp),
            isError = isError,
            modifier = Modifier.fillMaxWidth()
        )

        // Báo lỗi nhỏ nếu nhập thiếu
        if (isError) {
            Text(
                text = "Vui lòng nhập đầy đủ thông tin!",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp, start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- BUTTON: ĐĂNG NHẬP ---
        Button(
            onClick = {
                val cleanUser = username.trim()
                val cleanPass = password.trim()

                if (cleanUser.isNotEmpty() && cleanPass.isNotEmpty()) {
                    isLoading = true
                    performLogin(
                        context,
                        cleanUser,
                        cleanPass,
                        onSuccess = {
                            isLoading = false
                            onLoginSuccess()
                        },
                        onError = { isLoading = false }
                    )
                } else {
                    isError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "ĐĂNG NHẬP",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- FOOTER: ĐĂNG KÝ ---
        TextButton(onClick = onNavigateToRegister) {
            Text(
                text = "Chưa có tài khoản? Đăng ký ngay",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// --- LOGIC XỬ LÝ (ĐÃ SỬA user.avatarUrl) ---
fun performLogin(context: Context, user: String, pass: String, onSuccess: () -> Unit, onError: () -> Unit) {
    Log.d("DEBUG_LOGIN", "Đang gửi -> User: '$user' | Pass: '$pass'")

    val authService = RetrofitClient.instance
    val request = LoginRequest(user, pass)

    authService.login(request).enqueue(object : Callback<LoginResponse> {
        override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
            if (response.isSuccessful) {
                val loginData = response.body()
                val userData = loginData?.user

                if (loginData?.token != null && userData != null) {
                    val sharedPref = context.getSharedPreferences("ChatAppPrefs", Context.MODE_PRIVATE)

                    with(sharedPref.edit()) {
                        putString("TOKEN", loginData.token)
                        // Trong Model của bạn id có thể null, nên dùng userData.id ?: ""
                        putString("USER_ID", userData.id ?: "")
                        putString("USERNAME", userData.username ?: "")

                        // 🛠️ ĐÃ SỬA: Dùng .avatarUrl thay vì .avatar
                        putString("AVATAR", userData.avatarUrl ?: "")
                        putString("FULL_NAME", userData.fullName ?: "")

                        apply()
                    }

                    try {
                        com.example.client.api.SocketHandler.setSocket(loginData.token)
                        com.example.client.api.SocketHandler.establishConnection()
                    } catch (e: Exception) {
                        Log.e("LoginError", "Lỗi kết nối Socket: ${e.message}")
                    }

                    // 🛠️ ĐÃ SỬA: Log cũng dùng .avatarUrl
                    Log.d("LOGIN_SUCCESS", "Đã lưu User: ${userData.username}, Avatar: ${userData.avatarUrl}")
                    Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, "Lỗi: Dữ liệu trả về bị thiếu", Toast.LENGTH_SHORT).show()
                    onError()
                }
            } else {
                Log.e("LOGIN_FAIL", "Code: ${response.code()} - Message: ${response.message()}")
                Toast.makeText(context, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show()
                onError()
            }
        }
        override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
            Log.e("LOGIN_ERROR", "Lỗi mạng: ${t.message}")
            Toast.makeText(context, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
            onError()
        }
    })
}