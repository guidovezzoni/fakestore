package com.guidovezzoni.fakestore.ui.effect

import com.guidovezzoni.fakestore.ui.navigation.AppDestination

sealed interface MainUiEffect {
    data class NavigateToTab(val destination: AppDestination) : MainUiEffect
}
