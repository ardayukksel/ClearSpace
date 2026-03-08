package com.example.clearspace.data.model

data class UnlockApproval(
    val approvalId: String,
    val request: UnlockRequest,
    val friend: Friend,
    var respondedAt: Long? = null,
    var decision: ApprovalDecision = ApprovalDecision.NONE
)