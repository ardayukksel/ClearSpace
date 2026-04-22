package com.example.clearspace.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val user_name: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user_id: Int,
    val user_name: String,
    val email: String,
    val session_limit_minutes: Int? = null,
    val daily_limit_minutes: Int? = null,
    val points: Int? = null,
    val level: Int? = null,
    val current_streak: Int? = null,
    val longest_streak: Int? = null,
    val last_streak_date: String? = null
)

interface ApiService {

    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("users/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @GET("challenges/active")
    suspend fun getActiveChallenges(): List<ChallengeDto>

    @POST("sessions/start")
    suspend fun startSession(@Body request: StartSessionRequest): StartSessionResponse

    @POST("sessions/end")
    suspend fun endSession(@Body request: EndSessionRequest): EndSessionResponse

    @POST("sessions/update-duration")
    suspend fun updateSessionDuration(@Body request: UpdateSessionDurationRequest): GenericResponse

    @POST("user-challenges/complete")
    suspend fun completeChallenge(@Body request: CompleteChallengeRequest): CompleteChallengeResponse

    @GET("users/{userId}/streak")
    suspend fun getUserStreak(@Path("userId") userId: Int): GetUserStreakResponse

    @GET("users/{userId}/gamification")
    suspend fun getUserGamification(@Path("userId") userId: Int): GetUserGamificationResponse
}