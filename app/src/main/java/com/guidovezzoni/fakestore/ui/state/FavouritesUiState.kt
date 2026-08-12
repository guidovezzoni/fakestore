package com.guidovezzoni.fakestore.ui.state

sealed interface FavouritesUiState {
    data object Loading : FavouritesUiState
    data class Content(val products: List<ProductListItem>) : FavouritesUiState
    data object Error : FavouritesUiState
}
