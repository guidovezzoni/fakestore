package com.guidovezzoni.fakestore.ui.intent

sealed interface FavouritesUiIntent {
    data object LoadFavourites : FavouritesUiIntent
    data class ToggleFavourite(val productId: Int) : FavouritesUiIntent
}
