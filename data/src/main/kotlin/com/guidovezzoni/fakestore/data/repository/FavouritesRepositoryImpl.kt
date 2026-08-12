package com.guidovezzoni.fakestore.data.repository

import com.guidovezzoni.fakestore.data.database.FavouriteDao
import com.guidovezzoni.fakestore.data.database.FavouriteEntity
import com.guidovezzoni.fakestore.domain.repository.FavouritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavouritesRepositoryImpl(private val favouriteDao: FavouriteDao) : FavouritesRepository {

    override suspend fun addFavourite(productId: Int) {
        favouriteDao.insert(FavouriteEntity(productId = productId))
    }

    override suspend fun removeFavourite(productId: Int) {
        favouriteDao.delete(FavouriteEntity(productId = productId))
    }

    override fun getFavouriteIds(): Flow<Set<Int>> =
        favouriteDao.getAllIds().map { it.toSet() }
}
