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
    val rows_affected: Int,
    val streak: StreakDto?,
    val rewards: SessionRewardsDto?
)

data class UpdateSessionDurationRequest(
    val user_id: Int,
    val regulated_app: String
)

data class FindOrCreateUserRequest(
    val email: String,
    val password: String
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

data class StreakDto(
    val user_id: Int,
    val current_streak: Int,
    val longest_streak: Int,
    val last_streak_date: String?
)

data class RewardsDto(
    val challenge_points: Int,
    val streak_bonus: Int,
    val total_added: Int,
    val total_points: Int,
    val level: Int
)

data class SessionRewardsDto(
    val session_points: Int,
    val streak_bonus: Int,
    val total_added: Int,
    val total_points: Int,
    val level: Int
)

data class CompleteChallengeResponse(
    val success: Boolean,
    val message: String,
    val streak: StreakDto?,
    val rewards: RewardsDto?
)

data class GamificationDto(
    val user_id: Int,
    val user_name: String,
    val points: Int,
    val level: Int,
    val current_streak: Int,
    val longest_streak: Int,
    val last_streak_date: String?
)

data class GetUserStreakResponse(
    val success: Boolean,
    val streak: StreakDto
)

data class GetUserGamificationResponse(
    val success: Boolean,
    val gamification: GamificationDto
)

data class GenericResponse(
    val success: Boolean,
    val message: String
)