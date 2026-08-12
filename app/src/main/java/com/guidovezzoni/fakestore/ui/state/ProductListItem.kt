package com.guidovezzoni.fakestore.ui.state

data class ProductListItem(
    val id: Int,
    val imageUrl: String,
    val title: String,
    val formattedPrice: String,
    val formattedRatingScore: String,
    val isFavourite: Boolean = false,
)
