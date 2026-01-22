package com.example.client.view.navigation

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.client.view.screens.*
import com.example.client.viewmodel.ChatViewModel

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onUserLogin: (String) -> Unit,  // Nhận ID user khi login để Main đổi màu
    onUserLogout: () -> Unit        // Nhận lệnh logout để Main reset màu
) {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel()
    val context = LocalContext.current

    val sharedPref = context.getSharedPreferences("ChatAppPrefs", Context.MODE_PRIVATE)
    val savedToken = sharedPref.getString("TOKEN", null)
    val savedUserId = sharedPref.getString("USER_ID", null)

    val startDest = if (savedToken != null) "users" else "login"

    LaunchedEffect(savedToken, savedUserId) {
        if (savedToken != null && savedUserId != null) {
            chatViewModel.connect(savedToken, savedUserId)
        }
    }

    NavHost(navController = navController, startDestination = startDest) {

        // 1. LOGIN SCREEN
        composable("login") {
            LoginScreen(
                onLoginSuccess = { userId ->
                    // 🔥 QUAN TRỌNG: Báo cho MainActivity biết user nào vừa vào để đổi Theme
                    onUserLogin(userId)

                    navController.navigate("users") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        // 2. REGISTER SCREEN
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // 3. USERS SCREEN (Đã bỏ tham số gây lỗi pending_requests)
        composable("users") {
            UsersScreenImproved(
                viewModel = chatViewModel,
                onOpenChat = { roomId, roomName, isGroup, memberCount ->
                    if (isGroup && memberCount != null) {
                        navController.navigate("group/$roomId/${Uri.encode(roomName)}/$memberCount")
                    } else {
                        navController.navigate("chat/$roomId/${Uri.encode(roomName)}")
                    }
                },
                onOpenNewMessage = { navController.navigate("new_message") },
                onOpenProfile = { navController.navigate("profile") }
                // ⚠️ Đã xóa onOpenPendingRequests vì file UsersScreenImproved chưa hỗ trợ
            )
        }

        // 4. PROFILE SCREEN
        composable("profile") {
            ProfileScreen(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onLogout = {
                    // 🔥 QUAN TRỌNG: Báo cho MainActivity reset theme về Sáng
                    onUserLogout()

                    // Ngắt kết nối socket
                    chatViewModel.disconnect()

                    // Quay về Login
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 5. NEW MESSAGE
        composable("new_message") {
            NewMessageScreen(
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() },
                onUserSelected = { user ->
                    val room = chatViewModel.startPrivateChat(user)
                    navController.navigate("chat/${room.id}/${Uri.encode(room.name)}")
                },
                onAddContact = { navController.navigate("add_contact") },
                onCreateGroup = { name, ids ->
                    val room = chatViewModel.createGroup(name, ids)
                    navController.navigate("group/${room.id}/${Uri.encode(room.name)}/${ids.size + 1}")
                }
            )
        }

        // 6. ADD CONTACT
        composable("add_contact") {
            AddNewContactScreen(
                viewModel = chatViewModel, // 1. Thêm viewModel vào
                onBack = { navController.popBackStack() } // 2. Giữ nguyên nút Back
                // 3. Đã xóa onSave vì màn hình này tự xử lý lưu rồi
            )
        }

        // 7. CHAT SCREEN
        composable(
            route = "chat/{roomId}/{roomName}",
            arguments = listOf(navArgument("roomId") { type = NavType.StringType }, navArgument("roomName") { type = NavType.StringType })
        ) { backStackEntry ->
            ChatScreenImprovedScreen(
                roomId = backStackEntry.arguments?.getString("roomId") ?: "",
                roomName = Uri.decode(backStackEntry.arguments?.getString("roomName") ?: "Chat"),
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 8. GROUP CHAT
        composable(
            route = "group/{roomId}/{roomName}/{memberCount}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("roomName") { type = NavType.StringType },
                navArgument("memberCount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            ChatScreenImprovedScreen(
                roomId = backStackEntry.arguments?.getString("roomId") ?: "",
                roomName = Uri.decode(backStackEntry.arguments?.getString("roomName") ?: "Group"),
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}