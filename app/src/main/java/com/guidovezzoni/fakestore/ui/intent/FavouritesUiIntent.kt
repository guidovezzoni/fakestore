package com.guidovezzoni.fakestore.ui.intent

sealed interface FavouritesUiIntent {
    data object LoadFavourites : FavouritesUiIntent
    data object TrackScreenViewed : FavouritesUiIntent
    data class ToggleFavourite(val productId: Int) : FavouritesUiIntent
}
