package com.guidovezzoni.fakestore.ui.state

sealed interface ProductListUiState {
    data object Loading : ProductListUiState
    data class Content(val products: List<ProductListItem>) : ProductListUiState
    data object Error : ProductListUiState
}
