package com.guidovezzoni.fakestore.di

import android.content.Context
import androidx.room.Room
import com.guidovezzoni.fakestore.data.database.FavouriteDao
import com.guidovezzoni.fakestore.data.database.FavouritesDatabase
import com.guidovezzoni.fakestore.data.repository.FavouritesRepositoryImpl
import com.guidovezzoni.fakestore.domain.repository.FavouritesRepository
import com.guidovezzoni.fakestore.domain.usecase.GetFavouriteIdsUseCase
import com.guidovezzoni.fakestore.domain.usecase.ToggleFavouriteUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "favourites_database"

    @Provides
    @Singleton
    fun provideFavouritesDatabase(@ApplicationContext context: Context): FavouritesDatabase =
        Room.databaseBuilder(context, FavouritesDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideFavouriteDao(database: FavouritesDatabase): FavouriteDao =
        database.favouriteDao()

    @Provides
    fun provideFavouritesRepository(favouriteDao: FavouriteDao): FavouritesRepository =
        FavouritesRepositoryImpl(favouriteDao)

    @Provides
    fun provideToggleFavouriteUseCase(repository: FavouritesRepository): ToggleFavouriteUseCase =
        ToggleFavouriteUseCase(repository)

    @Provides
    fun provideGetFavouriteIdsUseCase(repository: FavouritesRepository): GetFavouriteIdsUseCase =
        GetFavouriteIdsUseCase(repository)
}
