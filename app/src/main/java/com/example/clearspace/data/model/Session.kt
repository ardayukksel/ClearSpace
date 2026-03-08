package com.example.clearspace.data.model

data class Session(
    val sessionId: String,
    val user: User,
    val app: RegulatedApp,
    val startTime: Long = System.currentTimeMillis(),
    var endTime: Long? = null,
    var durationSeconds: Int = 0,
    var breachedSessionLimit: Boolean = false,
    var breachCountAtStart: Int = 0,
    var status: SessionStatus = SessionStatus.ACTIVE
)