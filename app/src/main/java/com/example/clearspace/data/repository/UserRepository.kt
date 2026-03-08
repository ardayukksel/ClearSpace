package com.example.clearspace.data.repository

import com.example.clearspace.data.model.User

class UserRepository {

    private val users = mutableListOf<User>()

    fun addUser(user: User) {
        users.add(user)
    }

    fun getAllUsers(): List<User> {
        return users.toList()
    }

    fun getUserById(userId: String): User? {
        return users.find { it.userId == userId }
    }

    fun getUserByEmail(email: String): User? {
        return users.find { it.email.equals(email, ignoreCase = true) }
    }

    fun updateUser(updatedUser: User) {
        val index = users.indexOfFirst { it.userId == updatedUser.userId }
        if (index != -1) {
            users[index] = updatedUser
        }
    }

    fun removeUser(userId: String) {
        users.removeAll { it.userId == userId }
    }

    fun clearAll() {
        users.clear()
    }
}