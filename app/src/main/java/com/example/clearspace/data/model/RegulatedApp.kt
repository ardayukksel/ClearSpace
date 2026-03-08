package com.example.clearspace.data.model

data class RegulatedApp(
    val packageName: String,
    var displayName: String,
    var isEnabled: Boolean = true,
    var sessionLimitMinutes: Int = 10,
    var dailyLimitMinutes: Int = 60
)