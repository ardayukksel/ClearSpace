package com.example.clearspace.data.model

data class Notification(
    val notificationId: String,
    val user: User,
    val type: NotificationType,
    val title: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    var readAt: Long? = null
)