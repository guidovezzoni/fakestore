package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guidovezzoni.fakestore.ui.intent.ProfileUiIntent
import com.guidovezzoni.fakestore.ui.state.ProfileUiState
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Task 7.1
    @Test
    fun givenLoadingState_whenScreenRenders_thenLoadingIndicatorIsVisible_andNoContentOrErrorUiIsShown() {
        composeTestRule.setContent {
            FakeStoreTheme {
                ProfileScreen(uiState = ProfileUiState.Loading, onIntent = {})
            }
        }

        composeTestRule.onNodeWithTag(PROFILE_LOADING_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PROFILE_CONTENT_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PROFILE_ERROR_TEST_TAG).assertDoesNotExist()
    }

    // Task 7.2
    @Test
    fun givenContentState_whenScreenRenders_thenNameEmailAndFavouriteCountAreAllDisplayed() {
        val uiState = ProfileUiState.Content(
            fullName = FULL_NAME,
            email = EMAIL,
            favouriteCount = FAVOURITE_COUNT,
            initials = INITIALS,
        )

        composeTestRule.setContent {
            FakeStoreTheme {
                ProfileScreen(uiState = uiState, onIntent = {})
            }
        }

        composeTestRule.onNodeWithText(FULL_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText(EMAIL).assertIsDisplayed()
        composeTestRule.onNodeWithText(FAVOURITE_COUNT.toString()).assertIsDisplayed()
    }

    // Task 7.3
    @Test
    fun givenErrorState_whenScreenRenders_thenErrorMessageAndRetryButtonAreVisible() {
        composeTestRule.setContent {
            FakeStoreTheme {
                ProfileScreen(uiState = ProfileUiState.Error, onIntent = {})
            }
        }

        composeTestRule.onNodeWithText(ERROR_MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithText(RETRY_BUTTON_TEXT).assertIsDisplayed()
    }

    // Task 7.4
    @Test
    fun givenErrorState_whenRetryButtonIsTapped_thenRetryClickedIntentIsEmittedToOnIntent() {
        val capturedIntents = mutableListOf<ProfileUiIntent>()

        composeTestRule.setContent {
            FakeStoreTheme {
                ProfileScreen(
                    uiState = ProfileUiState.Error,
                    onIntent = { capturedIntents.add(it) },
                )
            }
        }

        composeTestRule.onNodeWithText(RETRY_BUTTON_TEXT).performClick()
        composeTestRule.waitForIdle()

        val expectedIntent = ProfileUiIntent.RetryClicked
        assertTrue(capturedIntents.contains(expectedIntent))
    }

    // Task 7.5
    @Test
    fun givenProfileScreen_whenComposedForFirstTime_thenLoadProfileAndTrackScreenViewedAreEachDispatchedExactlyOnce() {
        val capturedIntents = mutableListOf<ProfileUiIntent>()

        composeTestRule.setContent {
            FakeStoreTheme {
                ProfileScreen(
                    uiState = ProfileUiState.Loading,
                    onIntent = { capturedIntents.add(it) },
                )
            }
        }
        composeTestRule.waitForIdle()

        val expectedLoadProfileCount = 1
        val expectedTrackScreenViewedCount = 1
        assertEquals(
            expectedLoadProfileCount,
            capturedIntents.count { it == ProfileUiIntent.LoadProfile },
        )
        assertEquals(
            expectedTrackScreenViewedCount,
            capturedIntents.count { it == ProfileUiIntent.TrackScreenViewed },
        )
    }

    // Task 7.6
    @Test
    fun givenErrorState_whenRetryButtonIsInspectedByAccessibilityService_thenItIsFocusableHasNonEmptyLabelAndIsOperable() {
        composeTestRule.setContent {
            FakeStoreTheme {
                ProfileScreen(uiState = ProfileUiState.Error, onIntent = {})
            }
        }

        composeTestRule.onNodeWithText(RETRY_BUTTON_TEXT)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    private companion object {
        const val FULL_NAME = "John Doe"
        const val EMAIL = "john.doe@example.com"
        const val FAVOURITE_COUNT = 7
        const val INITIALS = "JD"
        const val ERROR_MESSAGE = "Something went wrong. Please try again."
        const val RETRY_BUTTON_TEXT = "Retry"
    }
}
