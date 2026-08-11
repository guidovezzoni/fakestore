package com.guidovezzoni.fakestore.ui.state

import com.guidovezzoni.fakestore.ui.navigation.AppDestination

data class MainUiState(
    val selectedDestination: AppDestination = AppDestination.Products,
)
