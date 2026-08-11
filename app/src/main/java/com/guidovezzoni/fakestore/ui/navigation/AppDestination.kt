package com.guidovezzoni.fakestore.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination {
    @Serializable
    data object Products : AppDestination

    @Serializable
    data object Favourites : AppDestination

    @Serializable
    data object Profile : AppDestination
}
