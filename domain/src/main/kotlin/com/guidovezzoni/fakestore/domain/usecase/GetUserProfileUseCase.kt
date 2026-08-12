package com.guidovezzoni.fakestore.domain.usecase

import com.guidovezzoni.fakestore.domain.model.UserProfile
import com.guidovezzoni.fakestore.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class GetUserProfileUseCase(private val repository: UserRepository) {
    operator fun invoke(id: Int): Flow<Result<UserProfile>> =
        flow { emit(Result.success(repository.getUserProfile(id))) }
            .catch { emit(Result.failure(it)) }
}
