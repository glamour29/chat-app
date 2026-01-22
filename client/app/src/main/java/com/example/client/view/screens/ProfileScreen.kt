package com.example.client.view.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ProfileScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("ChatAppPrefs", Context.MODE_PRIVATE)

    // Lấy thông tin user
    val username = sharedPref.getString("USERNAME", "User") ?: "User"
    // Lấy avatar (Ưu tiên ảnh vừa chọn trên máy -> sau đó đến ảnh từ server)
    val savedAvatarStr = sharedPref.getString("LOCAL_AVATAR", null)
        ?: sharedPref.getString("AVATAR_URL", "https://i.imgur.com/6VBx3io.png")

    // State hiển thị ảnh
    var currentAvatarUri by remember { mutableStateOf<Any?>(savedAvatarStr) }

    // Bộ chọn ảnh từ thư viện
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                // 1. Cập nhật giao diện ngay
                currentAvatarUri = uri
                // 2. Lưu đường dẫn ảnh vào máy
                sharedPref.edit().putString("LOCAL_AVATAR", uri.toString()).apply()
                Toast.makeText(context, "Đã lưu ảnh (Trên máy)", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding() // 🔥 QUAN TRỌNG: Đẩy nội dung xuống khỏi tai thỏ/thanh trạng thái
    ) {
        // --- HEADER: Nút Back và Tiêu đề ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Quản lý hồ sơ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // --- PHẦN AVATAR VÀ TÊN ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(120.dp)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(currentAvatarUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Avatar",
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = username,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Divider(thickness = 0.5.dp, color = Color.LightGray)

        // --- MENU SETTINGS ---
        Column(modifier = Modifier.padding(16.dp)) {

            // Item 1: Chế độ tối
            ProfileMenuItem(
                icon = Icons.Default.DarkMode,
                title = "Chế độ tối",
                trailing = {
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme() }
                    )
                },
                onClick = { onToggleTheme() }
            )

            // Item 2: Đăng xuất
            ProfileMenuItem(
                icon = Icons.Default.Logout,
                title = "Đăng xuất",
                textColor = Color.Red,
                iconColor = Color.Red,
                onClick = {
                    val editor = sharedPref.edit()

                    // 1. Xóa thông tin User (Bắt buộc)
                    editor.remove("TOKEN")
                    editor.remove("USER_ID")
                    editor.remove("USERNAME")
                    editor.remove("AVATAR_URL")
                    editor.remove("IS_DARK_MODE")
                    editor.remove("LOCAL_AVATAR")

                    editor.apply()

                    // 4. Ngắt kết nối socket
                    com.example.client.api.SocketHandler.closeConnection()

                    onLogout()
                }
            )
        }
    }
}

// Component con giữ nguyên
@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    iconColor: Color = MaterialTheme.colorScheme.onBackground,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconColor.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            fontSize = 16.sp,
            color = textColor,
            modifier = Modifier.weight(1f)
        )

        if (trailing != null) {
            trailing()
        }
    }
}