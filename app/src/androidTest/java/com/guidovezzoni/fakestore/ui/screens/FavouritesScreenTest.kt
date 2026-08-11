package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavouritesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenFavouritesScreen_whenComposed_thenFavouritesPlaceholderTextIsDisplayed() {
        composeTestRule.setContent {
            FakeStoreTheme {
                FavouritesScreen()
            }
        }

        composeTestRule.onNodeWithText(FAVOURITES_PLACEHOLDER_TEXT).assertIsDisplayed()
    }

    private companion object {
        const val FAVOURITES_PLACEHOLDER_TEXT = "Favourites coming soon…"
    }
}
