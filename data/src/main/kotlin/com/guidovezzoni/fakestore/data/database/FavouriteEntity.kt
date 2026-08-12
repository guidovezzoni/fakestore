package com.guidovezzoni.fakestore.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourite_entity")
data class FavouriteEntity(
    @PrimaryKey val productId: Int,
)
