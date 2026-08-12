package com.guidovezzoni.fakestore.domain.repository

import com.guidovezzoni.fakestore.domain.model.UserProfile

interface UserRepository {
    suspend fun getUserProfile(id: Int): UserProfile
}
