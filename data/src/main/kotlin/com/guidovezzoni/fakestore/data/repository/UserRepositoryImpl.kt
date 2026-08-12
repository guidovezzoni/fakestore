package com.guidovezzoni.fakestore.data.repository

import com.guidovezzoni.fakestore.data.mapper.UserMapper
import com.guidovezzoni.fakestore.data.network.ApiService
import com.guidovezzoni.fakestore.domain.model.UserProfile
import com.guidovezzoni.fakestore.domain.repository.UserRepository

class UserRepositoryImpl(private val apiService: ApiService) : UserRepository {
    override suspend fun getUserProfile(id: Int): UserProfile =
        UserMapper.map(apiService.getUser(id))
}
