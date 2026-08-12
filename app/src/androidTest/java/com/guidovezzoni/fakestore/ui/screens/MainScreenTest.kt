package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guidovezzoni.fakestore.HiltComponentActivity
import com.guidovezzoni.fakestore.ui.effect.MainUiEffect
import com.guidovezzoni.fakestore.ui.intent.MainUiIntent
import com.guidovezzoni.fakestore.ui.navigation.AppDestination
import com.guidovezzoni.fakestore.ui.state.MainUiState
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltComponentActivity>()

    @Test
    fun givenDefaultUiState_whenScreenIsComposed_thenProductsTabIsSelectedAndProductsTopAppBarIsDisplayed() {
        composeTestRule.setContent {
            FakeStoreTheme {
                MainScreen(
                    uiState = MainUiState(),
                    onIntent = {},
                    uiEffect = emptyFlow(),
                )
            }
        }

        composeTestRule.onNodeWithTag(BOTTOM_NAVIGATION_PRODUCTS_TAB_TEST_TAG).assertIsSelected()
        composeTestRule.onAllNodesWithText(PRODUCTS_TOP_APP_BAR_TITLE)[0].assertIsDisplayed()
    }

    @Test
    fun givenMainScreen_whenFavouritesTabTapped_thenFavouritesScreenIsDisplayedAndFavouritesTabIsSelected() {
        var uiState by mutableStateOf(MainUiState())
        // extraBufferCapacity = 1: tryEmit() is called synchronously on the test thread, but the
        // collector inside MainScreen runs asynchronously. Without a buffer, tryEmit() would drop
        // the emission if no collector is suspended and waiting at that exact instant.
        val uiEffect = MutableSharedFlow<MainUiEffect>(extraBufferCapacity = 1)

        composeTestRule.setContent {
            FakeStoreTheme {
                MainScreen(
                    uiState = uiState,
                    onIntent = { intent ->
                        when (intent) {
                            is MainUiIntent.TabTapped -> {
                                uiState = uiState.copy(selectedDestination = intent.destination)
                                uiEffect.tryEmit(MainUiEffect.NavigateToTab(intent.destination))
                            }
                        }
                    },
                    uiEffect = uiEffect,
                )
            }
        }

        composeTestRule.onNodeWithTag(BOTTOM_NAVIGATION_FAVOURITES_TAB_TEST_TAG).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FAVOURITES_LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_NAVIGATION_FAVOURITES_TAB_TEST_TAG).assertIsSelected()
    }

    @Test
    fun givenMainScreenWithFavouritesActive_whenFavouritesTabTappedAgain_thenFavouritesScreenRemainsDisplayedWithNoCrash() {
        var uiState by mutableStateOf(MainUiState())
        val uiEffect = MutableSharedFlow<MainUiEffect>(extraBufferCapacity = 1)

        composeTestRule.setContent {
            FakeStoreTheme {
                MainScreen(
                    uiState = uiState,
                    onIntent = { intent ->
                        when (intent) {
                            is MainUiIntent.TabTapped -> {
                                uiState = uiState.copy(selectedDestination = intent.destination)
                                uiEffect.tryEmit(MainUiEffect.NavigateToTab(intent.destination))
                            }
                        }
                    },
                    uiEffect = uiEffect,
                )
            }
        }

        composeTestRule.onNodeWithTag(BOTTOM_NAVIGATION_FAVOURITES_TAB_TEST_TAG).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(BOTTOM_NAVIGATION_FAVOURITES_TAB_TEST_TAG).performClick()
        composeTestRule.waitForIdle()

        // Verify no crash has occurred and the Favourites tab is still selected
        composeTestRule.onNodeWithTag(BOTTOM_NAVIGATION_FAVOURITES_TAB_TEST_TAG).assertIsSelected()
    }

    private companion object {
        const val PRODUCTS_TOP_APP_BAR_TITLE = "Products"
    }
}
