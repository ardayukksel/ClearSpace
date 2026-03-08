package com.example.clearspace.service

import com.example.clearspace.data.model.User
import java.util.UUID

class AuthService {

    private val users = mutableListOf<User>()
    private var currentUser: User? = null

    fun register(name: String, email: String): User {
        val user = User(
            userId = UUID.randomUUID().toString(),
            name = name,
            email = email
        )

        users.add(user)
        currentUser = user
        return user
    }

    fun login(email: String): User? {
        val user = users.find { it.email.equals(email, ignoreCase = true) }
        currentUser = user
        return user
    }

    fun logout() {
        currentUser = null
    }

    fun getCurrentUser(): User? {
        return currentUser
    }

    fun getAllUsers(): List<User> {
        return users
    }
}