package com.example.clearspace.data.repository

import com.example.clearspace.data.model.Challenge
import com.example.clearspace.data.model.ChallengeAttempt

class ChallengeRepository {

    private val challenges = mutableListOf<Challenge>()
    private val challengeAttempts = mutableListOf<ChallengeAttempt>()

    fun addChallenge(challenge: Challenge) {
        challenges.add(challenge)
    }

    fun addChallenges(challengeList: List<Challenge>) {
        challenges.addAll(challengeList)
    }

    fun getAllChallenges(): List<Challenge> {
        return challenges.toList()
    }

    fun getEnabledChallenges(): List<Challenge> {
        return challenges.filter { it.isEnabled }
    }

    fun getChallengeById(challengeId: String): Challenge? {
        return challenges.find { it.challengeId == challengeId }
    }

    fun updateChallenge(updatedChallenge: Challenge) {
        val index = challenges.indexOfFirst { it.challengeId == updatedChallenge.challengeId }
        if (index != -1) {
            challenges[index] = updatedChallenge
        }
    }

    fun addChallengeAttempt(attempt: ChallengeAttempt) {
        challengeAttempts.add(attempt)
    }

    fun getAllChallengeAttempts(): List<ChallengeAttempt> {
        return challengeAttempts.toList()
    }

    fun getAttemptsForSession(sessionId: String): List<ChallengeAttempt> {
        return challengeAttempts.filter { it.session.sessionId == sessionId }
    }

    fun clearAll() {
        challenges.clear()
        challengeAttempts.clear()
    }
}