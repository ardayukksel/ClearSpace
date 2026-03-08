package com.example.clearspace.manager

import com.example.clearspace.data.model.Session
import com.example.clearspace.data.model.User
import com.example.clearspace.service.BreachDetectionService

class LimitManager(
    private val breachDetectionService: BreachDetectionService = BreachDetectionService()
) {

    fun hasSessionLimitBeenReached(session: Session): Boolean {
        return breachDetectionService.hasSessionLimitBeenBreached(session)
    }

    fun hasDailyLimitBeenReached(user: User, sessions: List<Session>): Boolean {
        return breachDetectionService.hasDailyLimitBeenBreached(user, sessions)
    }

    fun shouldEscalate(user: User, sessions: List<Session>): Boolean {
        return breachDetectionService.shouldEscalate(user, sessions)
    }
}