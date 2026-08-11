package com.guidovezzoni.fakestore.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guidovezzoni.fakestore.core.analytics.AnalyticsClient
import com.guidovezzoni.fakestore.ui.effect.MainUiEffect
import com.guidovezzoni.fakestore.ui.intent.MainUiIntent
import com.guidovezzoni.fakestore.ui.navigation.AppDestination
import com.guidovezzoni.fakestore.ui.state.MainUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val analyticsClient: AnalyticsClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<MainUiEffect>()
    val uiEffect: SharedFlow<MainUiEffect> = _uiEffect.asSharedFlow()

    fun onIntent(intent: MainUiIntent) {
        when (intent) {
            is MainUiIntent.TabTapped -> onTabTapped(intent.destination)
        }
    }

    private fun onTabTapped(destination: AppDestination) {
        analyticsClient.logEvent(eventNameFor(destination))
        _uiState.value = _uiState.value.copy(selectedDestination = destination)
        viewModelScope.launch {
            _uiEffect.emit(MainUiEffect.NavigateToTab(destination))
        }
    }

    private fun eventNameFor(destination: AppDestination): String = when (destination) {
        AppDestination.Products -> EVENT_TAB_PRODUCTS_TAPPED
        AppDestination.Favourites -> EVENT_TAB_FAVOURITES_TAPPED
        AppDestination.Profile -> EVENT_TAB_PROFILE_TAPPED
    }

    private companion object {
        const val EVENT_TAB_PRODUCTS_TAPPED = "tab_products_tapped"
        const val EVENT_TAB_FAVOURITES_TAPPED = "tab_favourites_tapped"
        const val EVENT_TAB_PROFILE_TAPPED = "tab_profile_tapped"
    }
}
