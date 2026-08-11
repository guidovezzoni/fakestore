package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guidovezzoni.fakestore.ui.intent.ProductListUiIntent
import com.guidovezzoni.fakestore.ui.state.ProductListItem
import com.guidovezzoni.fakestore.ui.state.ProductListUiState
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createItem(
        id: Int = FIRST_PRODUCT_ID,
        title: String = FIRST_PRODUCT_TITLE,
    ) = ProductListItem(
        id = id,
        imageUrl = PRODUCT_IMAGE_URL,
        title = title,
        formattedPrice = FORMATTED_PRICE,
        formattedRatingScore = FORMATTED_RATING_SCORE,
    )

    @Test
    fun givenLoadingState_whenScreenIsComposed_thenLoadingIndicatorIsDisplayedAndNoProductCardsExist() {
        val uiState = ProductListUiState.Loading

        composeTestRule.setContent {
            FakeStoreTheme {
                ProductListScreen(uiState = uiState, onIntent = {})
            }
        }

        composeTestRule.onNodeWithTag(PRODUCT_LIST_LOADING_INDICATOR_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PRODUCT_LIST_ITEM_CARD_TEST_TAG_PREFIX + FIRST_PRODUCT_ID).assertDoesNotExist()
    }

    @Test
    fun givenListOfProducts_whenScreenIsDisplayed_thenOneCardIsShownPerProduct() {
        val items = listOf(
            createItem(id = FIRST_PRODUCT_ID, title = FIRST_PRODUCT_TITLE),
            createItem(id = SECOND_PRODUCT_ID, title = SECOND_PRODUCT_TITLE),
        )
        val uiState = ProductListUiState.Content(products = items)

        composeTestRule.setContent {
            FakeStoreTheme {
                ProductListScreen(uiState = uiState, onIntent = {})
            }
        }

        composeTestRule.onNodeWithTag(PRODUCT_LIST_ITEM_CARD_TEST_TAG_PREFIX + FIRST_PRODUCT_ID).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PRODUCT_LIST_ITEM_CARD_TEST_TAG_PREFIX + SECOND_PRODUCT_ID).assertIsDisplayed()
    }

    @Test
    fun givenEmptyContentState_whenScreenIsComposed_thenEmptyMessageIsDisplayedAndNoProductCardsExist() {
        val uiState = ProductListUiState.Content(products = emptyList())

        composeTestRule.setContent {
            FakeStoreTheme {
                ProductListScreen(uiState = uiState, onIntent = {})
            }
        }

        composeTestRule.onNodeWithTag(PRODUCT_LIST_EMPTY_MESSAGE_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PRODUCT_LIST_ITEM_CARD_TEST_TAG_PREFIX + FIRST_PRODUCT_ID).assertDoesNotExist()
    }

    @Test
    fun givenErrorState_whenScreenIsComposed_thenErrorMessageAndRetryButtonAreDisplayed() {
        val uiState = ProductListUiState.Error

        composeTestRule.setContent {
            FakeStoreTheme {
                ProductListScreen(uiState = uiState, onIntent = {})
            }
        }

        composeTestRule.onNodeWithText(ERROR_MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PRODUCT_LIST_RETRY_BUTTON_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun givenErrorState_whenRetryButtonIsTapped_thenRetryClickedIntentIsCaptured() {
        val capturedIntents = mutableListOf<ProductListUiIntent>()

        composeTestRule.setContent {
            FakeStoreTheme {
                ProductListScreen(
                    uiState = ProductListUiState.Error,
                    onIntent = { capturedIntents.add(it) },
                )
            }
        }

        composeTestRule.onNodeWithTag(PRODUCT_LIST_RETRY_BUTTON_TEST_TAG).performClick()
        composeTestRule.waitForIdle()

        assertTrue(capturedIntents.contains(ProductListUiIntent.RetryClicked))
    }

    @Test
    fun givenAnyState_whenScreenIsComposed_thenTopAppBarTitleIsAlwaysDisplayed() {
        var uiState: ProductListUiState by mutableStateOf(ProductListUiState.Loading)

        composeTestRule.setContent {
            FakeStoreTheme {
                ProductListScreen(uiState = uiState, onIntent = {})
            }
        }

        composeTestRule.onNodeWithText(TOP_APP_BAR_TITLE).assertIsDisplayed()

        uiState = ProductListUiState.Content(products = listOf(createItem()))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(TOP_APP_BAR_TITLE).assertIsDisplayed()

        uiState = ProductListUiState.Content(products = emptyList())
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(TOP_APP_BAR_TITLE).assertIsDisplayed()

        uiState = ProductListUiState.Error
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(TOP_APP_BAR_TITLE).assertIsDisplayed()
    }

    @Test
    fun givenScreenComposition_whenComposed_thenLoadProductsIntentIsFiredExactlyOnce() {
        val capturedIntents = mutableListOf<ProductListUiIntent>()
        val uiState = ProductListUiState.Loading

        composeTestRule.setContent {
            FakeStoreTheme {
                ProductListScreen(
                    uiState = uiState,
                    onIntent = { capturedIntents.add(it) },
                )
            }
        }
        composeTestRule.waitForIdle()

        val expectedLoadProductsCount = 1
        val result = capturedIntents.count { it == ProductListUiIntent.LoadProducts }
        assertEquals(expectedLoadProductsCount, result)
    }

    private companion object {
        const val FIRST_PRODUCT_ID = 1
        const val SECOND_PRODUCT_ID = 2
        const val FIRST_PRODUCT_TITLE = "First Product"
        const val SECOND_PRODUCT_TITLE = "Second Product"
        const val PRODUCT_IMAGE_URL = "https://example.com/image.png"
        const val FORMATTED_PRICE = "$109.95"
        const val FORMATTED_RATING_SCORE = "4.1"
        const val ERROR_MESSAGE = "Something went wrong. Please try again."
        const val TOP_APP_BAR_TITLE = "Products"
    }
}
