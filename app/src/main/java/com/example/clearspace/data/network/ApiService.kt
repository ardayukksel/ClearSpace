package com.example.clearspace.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @GET("challenges/active")
    suspend fun getActiveChallenges(): List<ChallengeDto>

    @POST("sessions/start")
    suspend fun startSession(@Body request: StartSessionRequest): StartSessionResponse

    @POST("sessions/end")
    suspend fun endSession(@Body request: EndSessionRequest): EndSessionResponse

    @POST("sessions/update-duration")
    suspend fun updateSessionDuration(@Body request: UpdateSessionDurationRequest): GenericResponse

    @POST("users/find-or-create")
    suspend fun findOrCreateUser(@Body request: FindOrCreateUserRequest): FindOrCreateUserResponse

    @POST("user-challenges/complete")
    suspend fun completeChallenge(@Body request: CompleteChallengeRequest): CompleteChallengeResponse

    @GET("users/{userId}/streak")
    suspend fun getUserStreak(@Path("userId") userId: Int): GetUserStreakResponse
}