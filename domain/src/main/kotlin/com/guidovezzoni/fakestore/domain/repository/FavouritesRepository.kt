package com.guidovezzoni.fakestore.domain.repository

import kotlinx.coroutines.flow.Flow

interface FavouritesRepository {
    suspend fun addFavourite(productId: Int)

    suspend fun removeFavourite(productId: Int)

    fun getFavouriteIds(): Flow<Set<Int>>
}
