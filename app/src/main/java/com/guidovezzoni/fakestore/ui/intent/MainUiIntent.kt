package com.guidovezzoni.fakestore.ui.intent

import com.guidovezzoni.fakestore.ui.navigation.AppDestination

sealed interface MainUiIntent {
    data class TabTapped(val destination: AppDestination) : MainUiIntent
}
