package com.example.clearspace.data.model

data class User(
    val userId: String,
    var name: String,
    var email: String,
    var points: Int = 0,
    var dailyLimitMinutes: Int = 60,
    var sessionLimitMinutes: Int = 10,
    var escalationThreshold: Int = 3,
    val createdAt: Long = System.currentTimeMillis()
)