package com.example.client.view.screens



import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.client.viewmodel.ChatViewModel
import com.example.client.view.components.MessageBubble

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    var textState by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Danh sách tin nhắn
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = false // Chỉnh thành true nếu muốn tin mới nhất ở dưới cùng khi load data cũ
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    isMq = message.senderId == viewModel.currentUserId
                )
            }
        }

        // Input nhập tin nhắn
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = textState,
                onValueChange = { textState = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập tin nhắn...") }
            )
            Button(
                onClick = {
                    viewModel.sendMessage(textState)
                    textState = ""
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Gửi")
            }
        }
    }
}