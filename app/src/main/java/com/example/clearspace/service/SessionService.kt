package com.example.clearspace.service

import com.example.clearspace.data.model.RegulatedApp
import com.example.clearspace.data.model.Session
import com.example.clearspace.data.model.SessionStatus
import com.example.clearspace.data.model.User
import java.util.UUID

class SessionService {

    private val sessions = mutableListOf<Session>()

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

        sessions.add(session)
        return session
    }

    fun endSession(session: Session): Session {
        val endTime = System.currentTimeMillis()
        val durationSeconds = ((endTime - session.startTime) / 1000).toInt()

        session.endTime = endTime
        session.durationSeconds = durationSeconds
        session.status = SessionStatus.ENDED

        return session
    }

    fun calculateDurationSeconds(session: Session): Int {
        val end = session.endTime ?: System.currentTimeMillis()
        return ((end - session.startTime) / 1000).toInt()
    }

    fun getAllSessions(): List<Session> {
        return sessions
    }

    fun getSessionsForUser(user: User): List<Session> {
        return sessions.filter { it.user.userId == user.userId }
    }

    fun getActiveSessions(): List<Session> {
        return sessions.filter { it.status == SessionStatus.ACTIVE }
    }

    fun getActiveSessionForApp(user: User, app: RegulatedApp): Session? {
        return sessions.find {
            it.user.userId == user.userId &&
                    it.app.packageName == app.packageName &&
                    it.status == SessionStatus.ACTIVE
        }
    }
}