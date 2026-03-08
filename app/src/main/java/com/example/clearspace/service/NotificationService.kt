package com.example.clearspace.service

import com.example.clearspace.data.model.Notification
import com.example.clearspace.data.model.NotificationType
import com.example.clearspace.data.model.UnlockRequest
import com.example.clearspace.data.model.User
import com.example.clearspace.data.repository.NotificationRepository
import java.util.UUID

class NotificationService(
    private val notificationRepository: NotificationRepository = NotificationRepository()
) {

    fun createNotification(
        user: User,
        type: NotificationType,
        title: String,
        body: String
    ): Notification {
        val notification = Notification(
            notificationId = UUID.randomUUID().toString(),
            user = user,
            type = type,
            title = title,
            body = body,
            createdAt = System.currentTimeMillis(),
            readAt = null
        )

        notificationRepository.addNotification(notification)
        return notification
    }

    fun createUnlockRequestNotification(user: User, request: UnlockRequest): Notification {
        return createNotification(
            user = user,
            type = NotificationType.UNLOCK_REQUEST,
            title = "Unlock Request Created",
            body = "An unlock request has been created for ${request.targetApp.displayName}."
        )
    }

    fun createWarningNotification(user: User, appName: String): Notification {
        return createNotification(
            user = user,
            type = NotificationType.WARNING,
            title = "Usage Limit Warning",
            body = "You have reached your limit for $appName."
        )
    }

    fun createReminderNotification(user: User, message: String): Notification {
        return createNotification(
            user = user,
            type = NotificationType.REMINDER,
            title = "Reminder",
            body = message
        )
    }

    fun markAsRead(notification: Notification) {
        notification.readAt = System.currentTimeMillis()
        notificationRepository.updateNotification(notification)
    }

    fun getNotificationsForUser(user: User): List<Notification> {
        return notificationRepository.getNotificationsForUser(user.userId)
    }

    fun getUnreadNotificationsForUser(user: User): List<Notification> {
        return notificationRepository.getUnreadNotificationsForUser(user.userId)
    }

    fun getAllNotifications(): List<Notification> {
        return notificationRepository.getAllNotifications()
    }
}