package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guidovezzoni.fakestore.ui.state.ProductListItem
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme
import org.junit.Assert.assertEquals
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
        isFavourite: Boolean = false,
    ) = ProductListItem(
        id = id,
        imageUrl = imageUrl,
        title = title,
        formattedPrice = formattedPrice,
        formattedRatingScore = formattedRatingScore,
        isFavourite = isFavourite,
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

    @Test
    fun givenFavouriteTrue_whenProductListItemCardIsDisplayed_thenFilledFavouriteIconIsShown() {
        val item = createItem(isFavourite = true)

        composeTestRule.setContent {
            FakeStoreTheme {
                ProductListItemCard(item = item)
            }
        }

        composeTestRule.onNodeWithContentDescription(FAVOURITE_REMOVE_CONTENT_DESCRIPTION).assertIsDisplayed()
    }

    @Test
    fun givenFavouriteFalse_whenProductListItemCardIsDisplayed_thenOutlinedFavouriteIconIsShown() {
        val item = createItem(isFavourite = false)

        composeTestRule.setContent {
            FakeStoreTheme {
                ProductListItemCard(item = item)
            }
        }

        composeTestRule.onNodeWithContentDescription(FAVOURITE_ADD_CONTENT_DESCRIPTION).assertIsDisplayed()
    }

    @Test
    fun givenProductId7_whenFavouriteIconIsTapped_thenOnToggleFavouriteIsInvokedWith7() {
        val item = createItem(id = TOGGLE_PRODUCT_ID, isFavourite = false)
        val invokedIds = mutableListOf<Int>()

        composeTestRule.setContent {
            FakeStoreTheme {
                ProductListItemCard(
                    item = item,
                    onToggleFavourite = { productId -> invokedIds.add(productId) },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(FAVOURITE_ADD_CONTENT_DESCRIPTION).performClick()

        val expectedInvokedIds = listOf(TOGGLE_PRODUCT_ID)
        assertEquals(expectedInvokedIds, invokedIds)
    }

    private companion object {
        const val PRODUCT_ID = 1
        const val TOGGLE_PRODUCT_ID = 7
        const val PRODUCT_TITLE = "Test Product"
        const val PRODUCT_IMAGE_URL = "https://example.com/image.png"
        const val FORMATTED_PRICE = "$109.95"
        const val FORMATTED_RATING_SCORE = "4.1"
        const val FAVOURITE_ADD_CONTENT_DESCRIPTION = "Add to favourites"
        const val FAVOURITE_REMOVE_CONTENT_DESCRIPTION = "Remove from favourites"
    }
}
