package com.example.healguard.model

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis()
)