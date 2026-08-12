package com.guidovezzoni.fakestore.ui.effect

sealed interface ProductListUiEffect {
    data object ShowFavouriteToggleError : ProductListUiEffect
}

