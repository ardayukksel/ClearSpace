package com.example.clearspace.data.model

data class UnlockRequest(
    val requestId: String,
    val user: User,
    val targetApp: RegulatedApp,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val requiredApprovals: Int = 1,
    var status: RequestStatus = RequestStatus.PENDING
)