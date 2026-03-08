package com.example.clearspace.data.repository

import com.example.clearspace.data.model.Session
import com.example.clearspace.data.model.SessionStatus

class SessionRepository {

    private val sessions = mutableListOf<Session>()

    fun addSession(session: Session) {
        sessions.add(session)
    }

    fun getAllSessions(): List<Session> {
        return sessions.toList()
    }

    fun getSessionById(sessionId: String): Session? {
        return sessions.find { it.sessionId == sessionId }
    }

    fun getSessionsForUser(userId: String): List<Session> {
        return sessions.filter { it.user.userId == userId }
    }

    fun getActiveSessions(): List<Session> {
        return sessions.filter { it.status == SessionStatus.ACTIVE }
    }

    fun getActiveSessionForUser(userId: String): Session? {
        return sessions.find {
            it.user.userId == userId && it.status == SessionStatus.ACTIVE
        }
    }

    fun updateSession(updatedSession: Session) {
        val index = sessions.indexOfFirst { it.sessionId == updatedSession.sessionId }
        if (index != -1) {
            sessions[index] = updatedSession
        }
    }

    fun removeSession(sessionId: String) {
        sessions.removeAll { it.sessionId == sessionId }
    }

    fun clearAll() {
        sessions.clear()
    }
}