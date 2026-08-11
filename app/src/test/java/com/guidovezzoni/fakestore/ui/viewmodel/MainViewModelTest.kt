package com.guidovezzoni.fakestore.ui.viewmodel

import com.guidovezzoni.fakestore.core.analytics.AnalyticsClient
import com.guidovezzoni.fakestore.ui.effect.MainUiEffect
import com.guidovezzoni.fakestore.ui.intent.MainUiIntent
import com.guidovezzoni.fakestore.ui.navigation.AppDestination
import io.mockk.MockKAnnotations
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        analyticsClient: AnalyticsClient = mockk(relaxed = true),
    ) = MainViewModel(analyticsClient)

    // Task 3.1
    @Test
    fun givenNewlyConstructedMainViewModel_whenNoIntentDispatched_thenSelectedDestinationIsProducts() {
        val expected = AppDestination.Products

        val viewModel = createViewModel()
        val result = viewModel.uiState.value.selectedDestination

        assertEquals(expected, result)
    }

    // Task 3.3
    @Test
    fun givenSelectedDestinationIsProducts_whenTabTappedFavourites_thenSelectedDestinationIsFavourites() {
        val expected = AppDestination.Favourites

        val viewModel = createViewModel()
        viewModel.onIntent(MainUiIntent.TabTapped(AppDestination.Favourites))
        val result = viewModel.uiState.value.selectedDestination

        assertEquals(expected, result)
    }

    // Task 3.4
    @Test
    fun givenSelectedDestinationIsProducts_whenTabTappedProducts_thenSelectedDestinationRemainsProducts() {
        val expected = AppDestination.Products

        val viewModel = createViewModel()
        viewModel.onIntent(MainUiIntent.TabTapped(AppDestination.Products))
        val result = viewModel.uiState.value.selectedDestination

        assertEquals(expected, result)
    }

    // Task 3.6
    @Test
    fun givenCollectorSubscribed_whenTabTappedProfile_thenUiEffectContainsNavigateToTabProfile() = runTest(testDispatcher) {
        val collectedEffects = mutableListOf<MainUiEffect>()
        val viewModel = createViewModel()

        val collectJob = launch {
            viewModel.uiEffect.collect { collectedEffects.add(it) }
        }

        viewModel.onIntent(MainUiIntent.TabTapped(AppDestination.Profile))

        collectJob.cancel()

        val expected = listOf(MainUiEffect.NavigateToTab(AppDestination.Profile))
        assertEquals(expected, collectedEffects)
    }

    // Task 3.8
    @Test
    fun givenMockedAnalyticsClient_whenTabTappedProducts_thenLogEventCalledOnceWithTabProductsTapped() {
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(analyticsClient = analyticsClient)

        viewModel.onIntent(MainUiIntent.TabTapped(AppDestination.Products))

        verify(exactly = 1) { analyticsClient.logEvent(name = "tab_products_tapped", params = emptyMap()) }
    }

    // Task 3.9
    @Test
    fun givenMockedAnalyticsClient_whenTabTappedFavourites_thenLogEventCalledOnceWithTabFavouritesTapped() {
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(analyticsClient = analyticsClient)

        viewModel.onIntent(MainUiIntent.TabTapped(AppDestination.Favourites))

        verify(exactly = 1) { analyticsClient.logEvent(name = "tab_favourites_tapped", params = emptyMap()) }
    }

    // Task 3.10
    @Test
    fun givenMockedAnalyticsClient_whenTabTappedProfile_thenLogEventCalledOnceWithTabProfileTapped() {
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(analyticsClient = analyticsClient)

        viewModel.onIntent(MainUiIntent.TabTapped(AppDestination.Profile))

        verify(exactly = 1) { analyticsClient.logEvent(name = "tab_profile_tapped", params = emptyMap()) }
    }

    // Task 3.11
    @Test
    fun givenMockedAnalyticsClient_whenTabTappedProductsTwice_thenLogEventCalledTwiceWithTabProductsTapped() {
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(analyticsClient = analyticsClient)

        viewModel.onIntent(MainUiIntent.TabTapped(AppDestination.Products))
        viewModel.onIntent(MainUiIntent.TabTapped(AppDestination.Products))

        verify(exactly = 2) { analyticsClient.logEvent(name = "tab_products_tapped", params = emptyMap()) }
    }
}
