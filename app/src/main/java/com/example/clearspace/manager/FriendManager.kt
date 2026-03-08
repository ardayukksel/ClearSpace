package com.example.clearspace.manager

import com.example.clearspace.data.model.ApprovalDecision
import com.example.clearspace.data.model.Friend
import com.example.clearspace.data.model.FriendStatus
import com.example.clearspace.data.model.UnlockApproval
import com.example.clearspace.data.model.UnlockRequest
import com.example.clearspace.data.model.User
import java.util.UUID

class FriendManager {

    private val friends = mutableListOf<Friend>()

    fun addFriend(user: User, displayName: String, contactHandle: String): Friend {
        val friend = Friend(
            friendId = UUID.randomUUID().toString(),
            user = user,
            displayName = displayName,
            contactHandle = contactHandle,
            status = FriendStatus.PENDING
        )

        friends.add(friend)
        return friend
    }

    fun acceptFriend(friend: Friend) {
        friend.status = FriendStatus.ACCEPTED
    }

    fun blockFriend(friend: Friend) {
        friend.status = FriendStatus.BLOCKED
    }

    fun getFriendsForUser(user: User): List<Friend> {
        return friends.filter { it.user.userId == user.userId }
    }

    fun getAcceptedFriendsForUser(user: User): List<Friend> {
        return friends.filter {
            it.user.userId == user.userId && it.status == FriendStatus.ACCEPTED
        }
    }

    fun createApproval(friend: Friend, request: UnlockRequest, decision: ApprovalDecision): UnlockApproval {
        return UnlockApproval(
            approvalId = UUID.randomUUID().toString(),
            request = request,
            friend = friend,
            respondedAt = System.currentTimeMillis(),
            decision = decision
        )
    }
}