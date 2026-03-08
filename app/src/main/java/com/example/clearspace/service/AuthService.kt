package com.example.clearspace.service

import com.example.clearspace.data.model.User
import com.example.clearspace.data.repository.UserRepository
import java.util.UUID

class AuthService(
    private val userRepository: UserRepository = UserRepository()
) {

    private var currentUser: User? = null

    fun register(name: String, email: String): User {
        val existingUser = userRepository.getUserByEmail(email)
        if (existingUser != null) {
            currentUser = existingUser
            return existingUser
        }

        val user = User(
            userId = UUID.randomUUID().toString(),
            name = name,
            email = email
        )

        userRepository.addUser(user)
        currentUser = user
        return user
    }

    fun login(email: String): User? {
        val user = userRepository.getUserByEmail(email)
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
        return userRepository.getAllUsers()
    }

    fun getUserById(userId: String): User? {
        return userRepository.getUserById(userId)
    }
}