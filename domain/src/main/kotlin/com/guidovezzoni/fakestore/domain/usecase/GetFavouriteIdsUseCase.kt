package com.guidovezzoni.fakestore.domain.usecase

import com.guidovezzoni.fakestore.domain.repository.FavouritesRepository
import kotlinx.coroutines.flow.Flow

class GetFavouriteIdsUseCase(private val repository: FavouritesRepository) {
    operator fun invoke(): Flow<Set<Int>> = repository.getFavouriteIds()
}
