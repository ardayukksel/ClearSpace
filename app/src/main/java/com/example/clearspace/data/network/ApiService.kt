package com.example.clearspace.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @GET("challenges/active")
    suspend fun getActiveChallenges(): List<ChallengeDto>

    @POST("sessions/start")
    suspend fun startSession(@Body request: StartSessionRequest): StartSessionResponse

    @POST("user-challenges/complete")
    suspend fun completeChallenge(@Body request: CompleteChallengeRequest): GenericResponse
}