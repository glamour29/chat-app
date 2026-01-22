package com.example.client.view.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.client.api.AuthService
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
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Chat App Login", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(40.dp))
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Tài khoản") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Mật khẩu") }, singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(imageVector = image, contentDescription = "Toggle Password") }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    performLogin(context, username, password, onSuccess = { isLoading = false; onLoginSuccess() }, onError = { isLoading = false })
                } else {
                    Toast.makeText(context, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            else Text(text = "ĐĂNG NHẬP", fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToRegister) { Text("Chưa có tài khoản? Đăng ký ngay") }
    }
}

fun performLogin(context: Context, user: String, pass: String, onSuccess: () -> Unit, onError: () -> Unit) {
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
                        putString("USER_ID", userData.id)
                        putString("USERNAME", userData.username)
                        apply()
                    }
                    com.example.client.api.SocketHandler.setSocket(loginData.token)
                    com.example.client.api.SocketHandler.establishConnection()
                    Log.d("LOGIN_SUCCESS", "Token: ${loginData.token}, ID: ${userData.id}")
                    Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, "Lỗi: Không nhận được thông tin người dùng", Toast.LENGTH_SHORT).show()
                    onError()
                }
            } else {
                Toast.makeText(context, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show()
                onError()
            }
        }
        override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
            Toast.makeText(context, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
            onError()
        }
    })
}
