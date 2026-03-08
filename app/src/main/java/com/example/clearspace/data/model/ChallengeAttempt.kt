package com.example.clearspace.data.model

data class ChallengeAttempt(
    val attemptId: String,
    val session: Session,
    val challenge: Challenge,
    val startedAt: Long = System.currentTimeMillis(),
    var completedAt: Long? = null,
    var outcome: AttemptOutcome = AttemptOutcome.FAILED,
    var timeSpentSeconds: Int = 0,
    var pointsAwarded: Int = 0
)