package com.example.clearspace.util

object TimeUtils {

    fun millisecondsToSeconds(milliseconds: Long): Int {
        return (milliseconds / 1000).toInt()
    }

    fun millisecondsToMinutes(milliseconds: Long): Int {
        return (milliseconds / (1000 * 60)).toInt()
    }

    fun minutesToMilliseconds(minutes: Int): Long {
        return minutes * 60 * 1000L
    }

    fun minutesToSeconds(minutes: Int): Int {
        return minutes * 60
    }

    fun isWithinLast24Hours(timestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        return (now - timestamp) <= oneDayMillis
    }
}