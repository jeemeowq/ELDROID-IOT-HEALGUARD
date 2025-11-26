package com.example.healguard.model


data class NotificationItem(
    var id: String = "",
    val type: String = "",
    val message: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val time: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)