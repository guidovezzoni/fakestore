package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guidovezzoni.fakestore.ui.navigation.AppDestination
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomNavigationBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenProductsSelectedDestination_whenBottomNavigationBarIsComposed_thenThreeTabsAreDisplayedWithLabels() {
        composeTestRule.setContent {
            FakeStoreTheme {
                BottomNavigationBar(
                    selectedDestination = AppDestination.Products,
                    onTabTap = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(BOTTOM_NAVIGATION_PRODUCTS_TAB_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_NAVIGATION_FAVOURITES_TAB_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BOTTOM_NAVIGATION_PROFILE_TAB_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(PRODUCTS_LABEL).assertIsDisplayed()
        composeTestRule.onNodeWithText(FAVOURITES_LABEL).assertIsDisplayed()
        composeTestRule.onNodeWithText(PROFILE_LABEL).assertIsDisplayed()
    }

    @Test
    fun givenFavouritesSelectedDestination_whenBottomNavigationBarIsComposed_thenFavouritesTabIsSelectedAndOthersAreNot() {
        composeTestRule.setContent {
            FakeStoreTheme {
                BottomNavigationBar(
                    selectedDestination = AppDestination.Favourites,
                    onTabTap = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(BOTTOM_NAVIGATION_FAVOURITES_TAB_TEST_TAG).assertIsSelected()
        composeTestRule.onNodeWithTag(BOTTOM_NAVIGATION_PRODUCTS_TAB_TEST_TAG).assertIsNotSelected()
        composeTestRule.onNodeWithTag(BOTTOM_NAVIGATION_PROFILE_TAB_TEST_TAG).assertIsNotSelected()
    }

    @Test
    fun givenProductsSelectedDestination_whenFavouritesTabIsTapped_thenOnTabTappedIsCalledWithFavourites() {
        val tappedDestinations = mutableListOf<AppDestination>()

        composeTestRule.setContent {
            FakeStoreTheme {
                BottomNavigationBar(
                    selectedDestination = AppDestination.Products,
                    onTabTap = { tappedDestinations.add(it) },
                )
            }
        }

        composeTestRule.onNodeWithTag(BOTTOM_NAVIGATION_FAVOURITES_TAB_TEST_TAG).performClick()
        composeTestRule.waitForIdle()

        assertTrue(tappedDestinations.contains(AppDestination.Favourites))
    }

    @Test
    fun givenProductsSelectedDestination_whenProductsTabIsTapped_thenOnTabTappedIsCalledWithProducts() {
        val tappedDestinations = mutableListOf<AppDestination>()

        composeTestRule.setContent {
            FakeStoreTheme {
                BottomNavigationBar(
                    selectedDestination = AppDestination.Products,
                    onTabTap = { tappedDestinations.add(it) },
                )
            }
        }

        composeTestRule.onNodeWithTag(BOTTOM_NAVIGATION_PRODUCTS_TAB_TEST_TAG).performClick()
        composeTestRule.waitForIdle()

        assertTrue(tappedDestinations.contains(AppDestination.Products))
    }

    private companion object {
        const val PRODUCTS_LABEL = "Products"
        const val FAVOURITES_LABEL = "Favourites"
        const val PROFILE_LABEL = "Profile"
    }
}
