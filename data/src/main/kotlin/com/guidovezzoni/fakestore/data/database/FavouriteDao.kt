package com.guidovezzoni.fakestore.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavouriteEntity)

    @Delete
    suspend fun delete(entity: FavouriteEntity)

    @Query("SELECT productId FROM favourite_entity")
    fun getAllIds(): Flow<List<Int>>
}
