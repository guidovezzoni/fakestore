package com.guidovezzoni.fakestore.ui.intent

sealed interface ProfileUiIntent {
    data object LoadProfile : ProfileUiIntent
    data object RetryClicked : ProfileUiIntent
    data object TrackScreenViewed : ProfileUiIntent
}
