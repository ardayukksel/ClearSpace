package com.example.clearspace.manager

import com.example.clearspace.data.model.RegulatedApp
import com.example.clearspace.data.model.Session
import com.example.clearspace.data.model.User
import com.example.clearspace.service.SessionService

class AppUsageManager(
    private val sessionService: SessionService = SessionService(),
    private val limitManager: LimitManager = LimitManager()
) {

    fun beginUsageSession(user: User, app: RegulatedApp): Session {
        return sessionService.startSession(user, app)
    }

    fun endUsageSession(session: Session): Session {
        return sessionService.endSession(session)
    }

    fun isSessionOverLimit(session: Session): Boolean {
        return limitManager.hasSessionLimitBeenReached(session)
    }

    fun isDailyUsageOverLimit(user: User, sessions: List<Session>): Boolean {
        return limitManager.hasDailyLimitBeenReached(user, sessions)
    }

    fun shouldTriggerEscalation(user: User, sessions: List<Session>): Boolean {
        return limitManager.shouldEscalate(user, sessions)
    }
}