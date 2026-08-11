package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guidovezzoni.fakestore.ui.intent.ProductListUiIntent
import com.guidovezzoni.fakestore.ui.state.ProductListItem
import com.guidovezzoni.fakestore.ui.state.ProductListUiState
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme
import org.junit.Assert.assertEquals
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
    fun givenListOfProducts_whenScreenIsDisplayed_thenOneCardIsShownPerProduct() {
        val items = listOf(
            createItem(id = FIRST_PRODUCT_ID, title = FIRST_PRODUCT_TITLE),
            createItem(id = SECOND_PRODUCT_ID, title = SECOND_PRODUCT_TITLE),
        )
        val uiState = ProductListUiState(products = items)

        composeTestRule.setContent {
            FakeStoreTheme {
                ProductListScreen(uiState = uiState, onIntent = {})
            }
        }

        composeTestRule.onNodeWithTag(PRODUCT_LIST_ITEM_CARD_TEST_TAG_PREFIX + FIRST_PRODUCT_ID).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PRODUCT_LIST_ITEM_CARD_TEST_TAG_PREFIX + SECOND_PRODUCT_ID).assertIsDisplayed()
    }

    @Test
    fun givenEmptyProductList_whenScreenIsDisplayed_thenNoProductCardsAreShown() {
        val uiState = ProductListUiState(products = emptyList())

        composeTestRule.setContent {
            FakeStoreTheme {
                ProductListScreen(uiState = uiState, onIntent = {})
            }
        }

        composeTestRule.onNodeWithTag(PRODUCT_LIST_ITEM_CARD_TEST_TAG_PREFIX + FIRST_PRODUCT_ID).assertDoesNotExist()
    }

    @Test
    fun givenScreenComposition_whenComposed_thenLoadProductsIntentIsFiredExactlyOnce() {
        val capturedIntents = mutableListOf<ProductListUiIntent>()
        val uiState = ProductListUiState()

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
    }
}
