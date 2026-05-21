package com.example.adoptus.data.model

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val fullName: String = "",
    val photoUrl: String = "",
    val role: String = "user",
    val createdAt: Any? = null
)