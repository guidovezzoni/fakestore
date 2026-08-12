package com.guidovezzoni.fakestore.ui.state

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Content(val fullName: String, val email: String, val favouriteCount: Int) : ProfileUiState
    data object Error : ProfileUiState
}
