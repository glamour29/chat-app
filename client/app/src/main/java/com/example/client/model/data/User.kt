package com.example.client.model.data

data class User(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val avatarUrl: String = "",
    val isOnline: Boolean = false
)