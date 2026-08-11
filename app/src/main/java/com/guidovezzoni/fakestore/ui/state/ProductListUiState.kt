package com.guidovezzoni.fakestore.ui.state

data class ProductListUiState(
    val products: List<ProductListItem> = emptyList(),
)
