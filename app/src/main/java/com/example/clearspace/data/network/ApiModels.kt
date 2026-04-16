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

data class EndSessionRequest(
    val user_id: Int,
    val regulated_app: String
)

data class EndSessionResponse(
    val success: Boolean,
    val message: String,
    val rows_affected: Int
)

data class UpdateSessionDurationRequest(
    val user_id: Int,
    val regulated_app: String
)

data class FindOrCreateUserRequest(
    val email: String
)

data class FindOrCreateUserResponse(
    val success: Boolean,
    val message: String,
    val user_id: Int,
    val user_name: String,
    val email: String
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