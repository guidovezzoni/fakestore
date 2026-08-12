package com.guidovezzoni.fakestore.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavouriteEntity::class], version = 1, exportSchema = true)
abstract class FavouritesDatabase : RoomDatabase() {
    abstract fun favouriteDao(): FavouriteDao
}
