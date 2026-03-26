package com.example.clearspace.data.network

data class ChallengeDto(
    val challenge_id: Int,
    val title: String,
    val description: String,
    val type: String
)

data class StartSessionRequest(
    val user_id: Int,
    val regulated_app: String
)

data class StartSessionResponse(
    val success: Boolean,
    val message: String,
    val session_id: Int
)

data class CompleteChallengeRequest(
    val user_id: Int,
    val challenge_id: Int,
    val result: String
)

data class GenericResponse(
    val success: Boolean,
    val message: String
)