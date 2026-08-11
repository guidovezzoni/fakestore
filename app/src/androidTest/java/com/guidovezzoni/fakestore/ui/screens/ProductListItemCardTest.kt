package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guidovezzoni.fakestore.ui.state.ProductListItem
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductListItemCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createItem(
        id: Int = PRODUCT_ID,
        imageUrl: String = PRODUCT_IMAGE_URL,
        title: String = PRODUCT_TITLE,
        formattedPrice: String = FORMATTED_PRICE,
        formattedRatingScore: String = FORMATTED_RATING_SCORE,
    ) = ProductListItem(
        id = id,
        imageUrl = imageUrl,
        title = title,
        formattedPrice = formattedPrice,
        formattedRatingScore = formattedRatingScore,
    )

    @Test
    fun givenProductItem_whenCardIsDisplayed_thenAllTextFieldsAreVisible() {
        val item = createItem()

        composeTestRule.setContent {
            FakeStoreTheme {
                ProductListItemCard(item = item)
            }
        }

        composeTestRule.onNodeWithText(item.title).assertIsDisplayed()
        composeTestRule.onNodeWithText(item.formattedPrice).assertIsDisplayed()
        composeTestRule.onNodeWithText(item.formattedRatingScore).assertIsDisplayed()
    }

    @Test
    fun givenProductItem_whenCardIsDisplayed_thenImageContentDescriptionIsLocalisedWithTitle() {
        val item = createItem()

        composeTestRule.setContent {
            FakeStoreTheme {
                ProductListItemCard(item = item)
            }
        }

        val expectedContentDescription = "Image of ${item.title}"
        composeTestRule.onNodeWithContentDescription(expectedContentDescription).assertIsDisplayed()
    }

    @Test
    fun givenProductItem_whenCardIsDisplayed_thenTestTagContainsItemId() {
        val item = createItem()

        composeTestRule.setContent {
            FakeStoreTheme {
                ProductListItemCard(item = item)
            }
        }

        composeTestRule.onNodeWithTag(PRODUCT_LIST_ITEM_CARD_TEST_TAG_PREFIX + item.id).assertIsDisplayed()
    }

    private companion object {
        const val PRODUCT_ID = 1
        const val PRODUCT_TITLE = "Test Product"
        const val PRODUCT_IMAGE_URL = "https://example.com/image.png"
        const val FORMATTED_PRICE = "$109.95"
        const val FORMATTED_RATING_SCORE = "4.1"
    }
}
