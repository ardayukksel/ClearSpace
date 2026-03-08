package com.example.clearspace.data.repository

import com.example.clearspace.data.model.Notification

class NotificationRepository {

    private val notifications = mutableListOf<Notification>()

    fun addNotification(notification: Notification) {
        notifications.add(notification)
    }

    fun getAllNotifications(): List<Notification> {
        return notifications.toList()
    }

    fun getNotificationsForUser(userId: String): List<Notification> {
        return notifications.filter { it.user.userId == userId }
    }

    fun getUnreadNotificationsForUser(userId: String): List<Notification> {
        return notifications.filter {
            it.user.userId == userId && it.readAt == null
        }
    }

    fun updateNotification(updatedNotification: Notification) {
        val index = notifications.indexOfFirst { it.notificationId == updatedNotification.notificationId }
        if (index != -1) {
            notifications[index] = updatedNotification
        }
    }

    fun clearAll() {
        notifications.clear()
    }
}