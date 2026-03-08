package com.example.clearspace.data.model

data class Challenge(
    val challengeId: String,
    val type: ChallengeType,
    val title: String,
    val description: String,
    val difficulty: Int,
    val rewardPoints: Int,
    var isEnabled: Boolean = true
)