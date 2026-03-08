package com.example.clearspace.data.model

data class Friend(
    val friendId: String,
    val user: User,
    var displayName: String,
    var contactHandle: String,
    var status: FriendStatus = FriendStatus.PENDING
)