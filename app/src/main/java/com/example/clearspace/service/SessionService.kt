package com.example.clearspace.service

import com.example.clearspace.data.model.RegulatedApp
import com.example.clearspace.data.model.Session
import com.example.clearspace.data.model.SessionStatus
import com.example.clearspace.data.model.User
import com.example.clearspace.data.repository.SessionRepository
import java.util.UUID

class SessionService(
    private val sessionRepository: SessionRepository = SessionRepository()
) {

    fun startSession(user: User, app: RegulatedApp): Session {
        val session = Session(
            sessionId = UUID.randomUUID().toString(),
            user = user,
            app = app,
            startTime = System.currentTimeMillis(),
            endTime = null,
            durationSeconds = 0,
            breachedSessionLimit = false,
            breachCountAtStart = 0,
            status = SessionStatus.ACTIVE
        )

        sessionRepository.addSession(session)
        return session
    }

    fun endSession(session: Session): Session {
        val endTime = System.currentTimeMillis()

        if (session.durationSeconds <= 0) {
            session.durationSeconds = ((endTime - session.startTime) / 1000).toInt()
        }

        session.endTime = endTime
        session.status = SessionStatus.ENDED

        sessionRepository.updateSession(session)
        return session
    }

    fun calculateDurationSeconds(session: Session): Int {
        if (session.durationSeconds > 0) {
            return session.durationSeconds
        }

        val end = session.endTime ?: System.currentTimeMillis()
        return ((end - session.startTime) / 1000).toInt()
    }

    fun getAllSessions(): List<Session> {
        return sessionRepository.getAllSessions()
    }

    fun getSessionsForUser(user: User): List<Session> {
        return sessionRepository.getSessionsForUser(user.userId)
    }

    fun getActiveSessions(): List<Session> {
        return sessionRepository.getActiveSessions()
    }

    fun getActiveSessionForUser(user: User): Session? {
        return sessionRepository.getActiveSessionForUser(user.userId)
    }
}