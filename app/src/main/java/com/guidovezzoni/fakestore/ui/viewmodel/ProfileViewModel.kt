package com.guidovezzoni.fakestore.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guidovezzoni.fakestore.core.analytics.AnalyticsClient
import com.guidovezzoni.fakestore.domain.model.UserProfile
import com.guidovezzoni.fakestore.domain.usecase.GetFavouriteIdsUseCase
import com.guidovezzoni.fakestore.domain.usecase.GetUserProfileUseCase
import com.guidovezzoni.fakestore.ui.effect.ProfileUiEffect
import com.guidovezzoni.fakestore.ui.intent.ProfileUiIntent
import com.guidovezzoni.fakestore.ui.state.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getFavouriteIdsUseCase: GetFavouriteIdsUseCase,
    private val analyticsClient: AnalyticsClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProfileUiEffect>()
    val uiEffect: SharedFlow<ProfileUiEffect> = _uiEffect.asSharedFlow()

    private val rawProfile = MutableStateFlow<UserProfile?>(null)

    init {
        viewModelScope.launch {
            combine(rawProfile.filterNotNull(), getFavouriteIdsUseCase()) { profile, favIds ->
                ProfileUiState.Content(
                    fullName = "${profile.name.firstName} ${profile.name.lastName}",
                    email = profile.email,
                    favouriteCount = favIds.size,
                )
            }.collect { _uiState.value = it }
        }
    }

    fun onIntent(intent: ProfileUiIntent) {
        when (intent) {
            is ProfileUiIntent.LoadProfile -> loadProfile()
            is ProfileUiIntent.RetryClicked -> retryProfile()
            is ProfileUiIntent.TrackScreenViewed -> trackScreenViewed()
        }
    }

    private fun loadProfile() {
        if (rawProfile.value != null) return
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            getUserProfileUseCase(USER_ID).collect { result ->
                result
                    .onSuccess { rawProfile.value = it }
                    .onFailure { _uiState.value = ProfileUiState.Error }
            }
        }
    }

    private fun retryProfile() {
        rawProfile.value = null
        loadProfile()
    }

    private fun trackScreenViewed() {
        viewModelScope.launch {
            val content = uiState.filterIsInstance<ProfileUiState.Content>().first()
            analyticsClient.logEvent(
                name = EVENT_PROFILE_SCREEN_VIEWED,
                params = mapOf(PARAM_FAVOURITE_COUNT to content.favouriteCount),
            )
        }
    }

    private companion object {
        // Currently, the user selection is not possible, the current user is hardcoded in order to
        // reduce the scope of the project and fit the deadline.
        const val USER_ID = 8
        const val EVENT_PROFILE_SCREEN_VIEWED = "profile_screen_viewed"
        const val PARAM_FAVOURITE_COUNT = "favourite_count"
    }
}
