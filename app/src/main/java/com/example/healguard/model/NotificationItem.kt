package com.example.healguard.model

import java.util.*

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "",
    val message: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val time: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)