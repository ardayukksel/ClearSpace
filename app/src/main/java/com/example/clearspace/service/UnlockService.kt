package com.example.clearspace.service

import com.example.clearspace.data.model.ApprovalDecision
import com.example.clearspace.data.model.Friend
import com.example.clearspace.data.model.RegulatedApp
import com.example.clearspace.data.model.RequestStatus
import com.example.clearspace.data.model.UnlockApproval
import com.example.clearspace.data.model.UnlockRequest
import com.example.clearspace.data.model.User
import com.example.clearspace.data.repository.UnlockRequestRepository
import java.util.UUID

class UnlockService(
    private val unlockRequestRepository: UnlockRequestRepository = UnlockRequestRepository()
) {

    fun createUnlockRequest(
        user: User,
        app: RegulatedApp,
        requiredApprovals: Int = 1,
        durationMinutes: Int = 30
    ): UnlockRequest {
        val now = System.currentTimeMillis()

        val request = UnlockRequest(
            requestId = UUID.randomUUID().toString(),
            user = user,
            targetApp = app,
            createdAt = now,
            expiresAt = now + (durationMinutes * 60 * 1000L),
            requiredApprovals = requiredApprovals,
            status = RequestStatus.PENDING
        )

        unlockRequestRepository.addUnlockRequest(request)
        return request
    }

    fun addApproval(
        request: UnlockRequest,
        friend: Friend,
        decision: ApprovalDecision
    ): UnlockApproval {
        val approval = UnlockApproval(
            approvalId = UUID.randomUUID().toString(),
            request = request,
            friend = friend,
            respondedAt = System.currentTimeMillis(),
            decision = decision
        )

        unlockRequestRepository.addApproval(approval)
        updateRequestStatus(request)
        unlockRequestRepository.updateUnlockRequest(request)

        return approval
    }

    fun getApprovalsForRequest(request: UnlockRequest): List<UnlockApproval> {
        return unlockRequestRepository.getApprovalsForRequest(request.requestId)
    }

    fun getRequestsForUser(user: User): List<UnlockRequest> {
        return unlockRequestRepository.getUnlockRequestsForUser(user.userId)
    }

    fun isRequestApproved(request: UnlockRequest): Boolean {
        val approvals = getApprovalsForRequest(request)
        val approveCount = approvals.count { it.decision == ApprovalDecision.APPROVE }
        return approveCount >= request.requiredApprovals
    }

    fun isRequestExpired(request: UnlockRequest): Boolean {
        return System.currentTimeMillis() > request.expiresAt
    }

    fun updateRequestStatus(request: UnlockRequest) {
        request.status = when {
            isRequestExpired(request) -> RequestStatus.EXPIRED
            isRequestApproved(request) -> RequestStatus.APPROVED
            else -> RequestStatus.PENDING
        }
    }

    fun rejectRequest(request: UnlockRequest) {
        request.status = RequestStatus.REJECTED
        unlockRequestRepository.updateUnlockRequest(request)
    }

    fun getAllRequests(): List<UnlockRequest> {
        return unlockRequestRepository.getAllUnlockRequests()
    }

    fun getAllApprovals(): List<UnlockApproval> {
        return unlockRequestRepository.getAllApprovals()
    }
}