package com.example.clearspace.service

import com.example.clearspace.data.model.AttemptOutcome
import com.example.clearspace.data.model.Challenge
import com.example.clearspace.data.model.ChallengeAttempt
import com.example.clearspace.data.model.ChallengeType
import com.example.clearspace.data.model.Session
import com.example.clearspace.data.repository.ChallengeRepository
import java.util.UUID

class ChallengeService(
    private val challengeRepository: ChallengeRepository = ChallengeRepository()
) {

    init {
        if (challengeRepository.getAllChallenges().isEmpty()) {
            seedDefaultChallenges()
        }
    }

    private fun seedDefaultChallenges() {
        val defaultChallenges = listOf(
            Challenge(
                challengeId = "c1",
                type = ChallengeType.BREATHING,
                title = "Take 3 Deep Breaths",
                description = "Pause and take three slow deep breaths before continuing.",
                difficulty = 1,
                rewardPoints = 5,
                isEnabled = true
            ),
            Challenge(
                challengeId = "c2",
                type = ChallengeType.MATH,
                title = "Quick Math Check",
                description = "Answer a simple math question to continue.",
                difficulty = 2,
                rewardPoints = 10,
                isEnabled = true
            ),
            Challenge(
                challengeId = "c3",
                type = ChallengeType.REFLECTION,
                title = "Reflect Before Scrolling",
                description = "State whether your app usage is intentional or mindless.",
                difficulty = 1,
                rewardPoints = 5,
                isEnabled = true
            ),
            Challenge(
                challengeId = "c4",
                type = ChallengeType.COUNTDOWN,
                title = "10 Second Pause",
                description = "Wait 10 seconds and reflect before continuing.",
                difficulty = 1,
                rewardPoints = 5,
                isEnabled = true
            )
        )

        challengeRepository.addChallenges(defaultChallenges)
    }

    fun getAllChallenges(): List<Challenge> {
        return challengeRepository.getAllChallenges()
    }

    fun getEnabledChallenges(): List<Challenge> {
        return challengeRepository.getEnabledChallenges()
    }

    fun getRandomChallenge(): Challenge {
        return getEnabledChallenges().random()
    }

    fun createChallengeAttempt(session: Session, challenge: Challenge): ChallengeAttempt {
        val attempt = ChallengeAttempt(
            attemptId = UUID.randomUUID().toString(),
            session = session,
            challenge = challenge,
            startedAt = System.currentTimeMillis(),
            completedAt = null,
            outcome = AttemptOutcome.FAILED,
            timeSpentSeconds = 0,
            pointsAwarded = 0
        )

        challengeRepository.addChallengeAttempt(attempt)
        return attempt
    }

    fun completeChallengeAttempt(attempt: ChallengeAttempt): ChallengeAttempt {
        val completedAt = System.currentTimeMillis()
        val timeSpentSeconds = ((completedAt - attempt.startedAt) / 1000).toInt()

        attempt.completedAt = completedAt
        attempt.outcome = AttemptOutcome.COMPLETED
        attempt.timeSpentSeconds = timeSpentSeconds
        attempt.pointsAwarded = attempt.challenge.rewardPoints

        return attempt
    }

    fun skipChallengeAttempt(attempt: ChallengeAttempt): ChallengeAttempt {
        val completedAt = System.currentTimeMillis()
        val timeSpentSeconds = ((completedAt - attempt.startedAt) / 1000).toInt()

        attempt.completedAt = completedAt
        attempt.outcome = AttemptOutcome.SKIPPED
        attempt.timeSpentSeconds = timeSpentSeconds
        attempt.pointsAwarded = 0

        return attempt
    }

    fun getAttemptsForSession(session: Session): List<ChallengeAttempt> {
        return challengeRepository.getAttemptsForSession(session.sessionId)
    }

    fun getAllAttempts(): List<ChallengeAttempt> {
        return challengeRepository.getAllChallengeAttempts()
    }
}