package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guidovezzoni.fakestore.ui.effect.FavouritesUiEffect
import com.guidovezzoni.fakestore.ui.intent.FavouritesUiIntent
import com.guidovezzoni.fakestore.ui.state.FavouritesUiState
import com.guidovezzoni.fakestore.ui.state.ProductListItem
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavouritesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createItem(
        id: Int = FIRST_PRODUCT_ID,
        title: String = FIRST_PRODUCT_TITLE,
        isFavourite: Boolean = true,
    ) = ProductListItem(
        id = id,
        imageUrl = PRODUCT_IMAGE_URL,
        title = title,
        formattedPrice = FORMATTED_PRICE,
        formattedRatingScore = FORMATTED_RATING_SCORE,
        isFavourite = isFavourite,
    )

    @Test
    fun givenLoadingState_whenFavouritesScreenIsComposed_thenLoadingIndicatorIsDisplayedAndNoProductCardsOrEmptyMessageExist() {
        val uiState = FavouritesUiState.Loading

        composeTestRule.setContent {
            FakeStoreTheme {
                FavouritesScreen(uiState = uiState, onIntent = {})
            }
        }

        composeTestRule.onNodeWithTag(FAVOURITES_LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PRODUCT_LIST_ITEM_CARD_TEST_TAG_PREFIX + FIRST_PRODUCT_ID).assertDoesNotExist()
        composeTestRule.onNodeWithTag(FAVOURITES_EMPTY_MESSAGE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun givenContentStateWithProducts_whenFavouritesScreenIsComposed_thenOneCardPerProductIsDisplayed() {
        val items = listOf(
            createItem(id = FIRST_PRODUCT_ID, title = FIRST_PRODUCT_TITLE),
            createItem(id = SECOND_PRODUCT_ID, title = SECOND_PRODUCT_TITLE),
        )
        val uiState = FavouritesUiState.Content(products = items)

        composeTestRule.setContent {
            FakeStoreTheme {
                FavouritesScreen(uiState = uiState, onIntent = {})
            }
        }

        composeTestRule.onNodeWithTag(PRODUCT_LIST_ITEM_CARD_TEST_TAG_PREFIX + FIRST_PRODUCT_ID).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PRODUCT_LIST_ITEM_CARD_TEST_TAG_PREFIX + SECOND_PRODUCT_ID).assertIsDisplayed()
    }

    @Test
    fun givenEmptyContentState_whenFavouritesScreenIsComposed_thenEmptyMessageIsDisplayedAndNoProductCardsExist() {
        val uiState = FavouritesUiState.Content(products = emptyList())

        composeTestRule.setContent {
            FakeStoreTheme {
                FavouritesScreen(uiState = uiState, onIntent = {})
            }
        }

        composeTestRule.onNodeWithTag(FAVOURITES_EMPTY_MESSAGE_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PRODUCT_LIST_ITEM_CARD_TEST_TAG_PREFIX + FIRST_PRODUCT_ID).assertDoesNotExist()
    }

    @Test
    fun givenContentStateWithProductId7_whenHeartIconIsTapped_thenToggleFavouriteIntentIsDispatched() {
        val capturedIntents = mutableListOf<FavouritesUiIntent>()
        val item = createItem(id = PRODUCT_ID_7, title = PRODUCT_TITLE_7, isFavourite = true)
        val uiState = FavouritesUiState.Content(products = listOf(item))

        composeTestRule.setContent {
            FakeStoreTheme {
                FavouritesScreen(
                    uiState = uiState,
                    onIntent = { capturedIntents.add(it) },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(REMOVE_FROM_FAVOURITES_CONTENT_DESCRIPTION).performClick()
        composeTestRule.waitForIdle()

        val expectedIntent = FavouritesUiIntent.ToggleFavourite(productId = PRODUCT_ID_7)
        assertTrue(capturedIntents.contains(expectedIntent))
    }

    @Test
    fun givenFavouritesScreen_whenComposed_thenLoadFavouritesIntentIsFiredExactlyOnce() {
        val capturedIntents = mutableListOf<FavouritesUiIntent>()
        val uiState = FavouritesUiState.Loading

        composeTestRule.setContent {
            FakeStoreTheme {
                FavouritesScreen(
                    uiState = uiState,
                    onIntent = { capturedIntents.add(it) },
                )
            }
        }
        composeTestRule.waitForIdle()

        val expectedLoadFavouritesCount = 1
        val result = capturedIntents.count { it == FavouritesUiIntent.LoadFavourites }
        assertEquals(expectedLoadFavouritesCount, result)
    }

    // Task 4.1
    @Test
    fun givenEmptyContentState_whenFavouritesScreenIsComposed_thenEmptyMessageContainsInstructionalText() {
        val uiState = FavouritesUiState.Content(products = emptyList())

        composeTestRule.setContent {
            FakeStoreTheme {
                FavouritesScreen(uiState = uiState, onIntent = {})
            }
        }

        composeTestRule.onNodeWithText(FAVOURITES_EMPTY_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(FAVOURITES_EMPTY_SUBTITLE).assertIsDisplayed()
    }

    // Task 3.1
    @Test
    fun givenFavouritesScreen_whenComposed_thenTrackScreenViewedIntentIsFiredExactlyOnce() {
        val capturedIntents = mutableListOf<FavouritesUiIntent>()
        val uiState = FavouritesUiState.Loading

        composeTestRule.setContent {
            FakeStoreTheme {
                FavouritesScreen(
                    uiState = uiState,
                    onIntent = { capturedIntents.add(it) },
                )
            }
        }
        composeTestRule.waitForIdle()

        val expectedTrackScreenViewedCount = 1
        val result = capturedIntents.count { it == FavouritesUiIntent.TrackScreenViewed }
        assertEquals(expectedTrackScreenViewedCount, result)
    }

    // Task 3.2
    @Test
    fun givenErrorState_whenFavouritesScreenIsComposed_thenErrorMessageIsDisplayed() {
        val uiState = FavouritesUiState.Error

        composeTestRule.setContent {
            FakeStoreTheme {
                FavouritesScreen(uiState = uiState, onIntent = {})
            }
        }

        composeTestRule.onNodeWithText(PRODUCT_LIST_ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun givenShowFavouriteToggleErrorEffect_whenEffectIsCollected_thenSnackbarWithErrorMessageIsShown() {
        val uiEffect = MutableSharedFlow<FavouritesUiEffect>(extraBufferCapacity = 1)

        composeTestRule.setContent {
            FakeStoreTheme {
                FavouritesScreen(
                    uiState = FavouritesUiState.Loading,
                    onIntent = {},
                    uiEffect = uiEffect,
                )
            }
        }

        uiEffect.tryEmit(FavouritesUiEffect.ShowFavouriteToggleError)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(FAVOURITE_TOGGLE_ERROR_MESSAGE).assertIsDisplayed()
    }

    private companion object {
        const val FIRST_PRODUCT_ID = 1
        const val SECOND_PRODUCT_ID = 2
        const val PRODUCT_ID_7 = 7
        const val FIRST_PRODUCT_TITLE = "First Favourite"
        const val SECOND_PRODUCT_TITLE = "Second Favourite"
        const val PRODUCT_TITLE_7 = "Favourite Product 7"
        const val PRODUCT_IMAGE_URL = "https://example.com/image.png"
        const val FORMATTED_PRICE = "$109.95"
        const val FORMATTED_RATING_SCORE = "4.1"
        const val FAVOURITE_TOGGLE_ERROR_MESSAGE = "Unable to update favourite"
        const val PRODUCT_LIST_ERROR_MESSAGE = "Something went wrong. Please try again."
        const val FAVOURITES_EMPTY_TITLE = "No favourites yet"
        const val FAVOURITES_EMPTY_SUBTITLE = "Tap the heart on a product to save it here."
        const val REMOVE_FROM_FAVOURITES_CONTENT_DESCRIPTION = "Remove from favourites"
    }
}
