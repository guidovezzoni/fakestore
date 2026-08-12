package com.guidovezzoni.fakestore.ui.effect

sealed interface FavouritesUiEffect {
    data object ShowFavouriteToggleError : FavouritesUiEffect
}
