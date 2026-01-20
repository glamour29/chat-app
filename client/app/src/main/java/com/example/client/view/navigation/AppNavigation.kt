// kotlin
package com.example.client.view.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.client.view.screens.*
import com.example.client.viewmodel.ChatViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel()

    NavHost(navController = navController, startDestination = "users") {

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
                onOpenNewMessage = { navController.navigate("new_message") }
            )
        }

        composable("new_message") {
            NewMessageScreen(
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() },
                onUserSelected = { user ->
                    val room = chatViewModel.startPrivateChat(user)
                    navController.navigate("chat/${room.id}/${Uri.encode(room.name)}")
                },
                onAddContact = { navController.navigate("add_contact") },
                onCreateGroup = { groupName, memberIds ->
                    val room = chatViewModel.createGroup(groupName, memberIds)
                    navController.navigate("group/${room.id}/${Uri.encode(room.name)}/${room.memberIds.size}")
                }
            )
        }

        composable("add_contact") {
            AddNewContactScreen(
                onBack = { navController.popBackStack() },
                onSave = { _, _ -> navController.popBackStack() }
            )
        }

        composable(
            route = "chat/{roomId}/{roomName}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("roomName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val roomName = backStackEntry.arguments?.getString("roomName") ?: "Chat"
            ChatScreenImprovedScreen(
                roomId = roomId,
                roomName = Uri.decode(roomName),
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "group/{roomId}/{roomName}/{memberCount}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("roomName") { type = NavType.StringType },
                navArgument("memberCount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val roomName = backStackEntry.arguments?.getString("roomName") ?: "Group"
            // Use the same screen composable that was renamed to ChatScreenImprovedScreen.
            ChatScreenImprovedScreen(
                roomId = roomId,
                roomName = Uri.decode(roomName),
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}