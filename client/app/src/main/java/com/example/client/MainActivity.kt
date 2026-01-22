package com.example.client

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.client.view.navigation.AppNavigation
import com.example.client.view.theme.ClientTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val sharedPref = getSharedPreferences("ChatAppPrefs", Context.MODE_PRIVATE)

            // Hàm hỗ trợ: Lấy theme theo ID người dùng
            // Nếu userId là null (chưa đăng nhập) -> Lấy theo giao diện điện thoại (System)
            fun getThemeForUser(userId: String?): Boolean {
                if (userId == null) return false // Mặc định Sáng khi ở màn hình Login (hoặc dùng isSystemInDarkTheme())
                // Lấy cài đặt riêng của user đó, nếu chưa set thì mặc định Sáng (false)
                return sharedPref.getBoolean("DARK_MODE_$userId", false)
            }

            // 1. Khởi tạo trạng thái ban đầu
            val currentUserId = sharedPref.getString("USER_ID", null)
            var isDarkTheme by remember { mutableStateOf(getThemeForUser(currentUserId)) }

            ClientTheme(darkTheme = isDarkTheme) {
                AppNavigation(
                    isDarkTheme = isDarkTheme,

                    // 2. Khi bấm nút đổi màu trong Profile
                    onToggleTheme = {
                        val newMode = !isDarkTheme
                        isDarkTheme = newMode // Đổi màu ngay lập tức

                        // LƯU RIÊNG CHO USER HIỆN TẠI
                        val userId = sharedPref.getString("USER_ID", null)
                        if (userId != null) {
                            sharedPref.edit().putBoolean("DARK_MODE_$userId", newMode).apply()
                        }
                    },

                    // 3. Callback mới: Khi Đăng nhập thành công -> Load theme của user đó
                    onUserLogin = { userId ->
                        isDarkTheme = getThemeForUser(userId)
                    },

                    // 4. Callback mới: Khi Đăng xuất -> Reset về mặc định
                    onUserLogout = {
                        isDarkTheme = false // Về chế độ Sáng (hoặc System) khi logout
                    }
                )
            }
        }
    }
}