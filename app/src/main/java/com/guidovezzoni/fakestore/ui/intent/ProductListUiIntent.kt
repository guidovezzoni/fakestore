package com.guidovezzoni.fakestore.ui.intent

sealed interface ProductListUiIntent {
    data object LoadProducts : ProductListUiIntent
    data object RetryClicked : ProductListUiIntent
}
