package com.example.clearspace.service

import com.example.clearspace.data.model.Session
import com.example.clearspace.data.model.User

class BreachDetectionService {

    fun hasSessionLimitBeenBreached(session: Session): Boolean {
        val sessionLimitSeconds = session.app.sessionLimitMinutes * 60
        return session.durationSeconds >= sessionLimitSeconds
    }

    fun markSessionBreach(session: Session): Session {
        session.breachedSessionLimit = true
        return session
    }

    fun countBreachesForUserToday(user: User, sessions: List<Session>): Int {
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L

        return sessions.count {
            it.user.userId == user.userId &&
                    it.breachedSessionLimit &&
                    (now - it.startTime) <= oneDayMillis
        }
    }

    fun shouldEscalate(user: User, sessions: List<Session>): Boolean {
        val breachesToday = countBreachesForUserToday(user, sessions)
        return breachesToday >= user.escalationThreshold
    }

    fun hasDailyLimitBeenBreached(user: User, sessions: List<Session>): Boolean {
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L

        val totalMinutesToday = sessions
            .filter { it.user.userId == user.userId && (now - it.startTime) <= oneDayMillis }
            .sumOf { it.durationSeconds } / 60

        return totalMinutesToday >= user.dailyLimitMinutes
    }
}