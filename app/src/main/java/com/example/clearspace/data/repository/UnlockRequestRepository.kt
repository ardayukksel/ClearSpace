package com.example.clearspace.data.repository

import com.example.clearspace.data.model.RequestStatus
import com.example.clearspace.data.model.UnlockApproval
import com.example.clearspace.data.model.UnlockRequest

class UnlockRequestRepository {

    private val unlockRequests = mutableListOf<UnlockRequest>()
    private val unlockApprovals = mutableListOf<UnlockApproval>()

    fun addUnlockRequest(request: UnlockRequest) {
        unlockRequests.add(request)
    }

    fun getAllUnlockRequests(): List<UnlockRequest> {
        return unlockRequests.toList()
    }

    fun getUnlockRequestById(requestId: String): UnlockRequest? {
        return unlockRequests.find { it.requestId == requestId }
    }

    fun getUnlockRequestsForUser(userId: String): List<UnlockRequest> {
        return unlockRequests.filter { it.user.userId == userId }
    }

    fun getPendingRequests(): List<UnlockRequest> {
        return unlockRequests.filter { it.status == RequestStatus.PENDING }
    }

    fun updateUnlockRequest(updatedRequest: UnlockRequest) {
        val index = unlockRequests.indexOfFirst { it.requestId == updatedRequest.requestId }
        if (index != -1) {
            unlockRequests[index] = updatedRequest
        }
    }

    fun addApproval(approval: UnlockApproval) {
        unlockApprovals.add(approval)
    }

    fun getApprovalsForRequest(requestId: String): List<UnlockApproval> {
        return unlockApprovals.filter { it.request.requestId == requestId }
    }

    fun getAllApprovals(): List<UnlockApproval> {
        return unlockApprovals.toList()
    }

    fun clearAll() {
        unlockRequests.clear()
        unlockApprovals.clear()
    }
}