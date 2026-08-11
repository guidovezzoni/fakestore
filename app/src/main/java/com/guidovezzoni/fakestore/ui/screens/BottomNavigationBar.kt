package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.guidovezzoni.fakestore.R
import com.guidovezzoni.fakestore.ui.navigation.AppDestination
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme

const val BOTTOM_NAVIGATION_PRODUCTS_TAB_TEST_TAG = "bottom_navigation_products_tab"
const val BOTTOM_NAVIGATION_FAVOURITES_TAB_TEST_TAG = "bottom_navigation_favourites_tab"
const val BOTTOM_NAVIGATION_PROFILE_TAB_TEST_TAG = "bottom_navigation_profile_tab"

@Composable
fun BottomNavigationBar(
    selectedDestination: AppDestination,
    onTabTap: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = selectedDestination == AppDestination.Products,
            onClick = { onTabTap(AppDestination.Products) },
            icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
            label = { Text(stringResource(R.string.global_tab_products)) },
            modifier = Modifier.testTag(BOTTOM_NAVIGATION_PRODUCTS_TAB_TEST_TAG),
        )
        NavigationBarItem(
            selected = selectedDestination == AppDestination.Favourites,
            onClick = { onTabTap(AppDestination.Favourites) },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            label = { Text(stringResource(R.string.global_tab_favourites)) },
            modifier = Modifier.testTag(BOTTOM_NAVIGATION_FAVOURITES_TAB_TEST_TAG),
        )
        NavigationBarItem(
            selected = selectedDestination == AppDestination.Profile,
            onClick = { onTabTap(AppDestination.Profile) },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text(stringResource(R.string.global_tab_profile)) },
            modifier = Modifier.testTag(BOTTOM_NAVIGATION_PROFILE_TAB_TEST_TAG),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewBottomNavigationBarProducts() {
    FakeStoreTheme {
        BottomNavigationBar(
            selectedDestination = AppDestination.Products,
            onTabTap = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewBottomNavigationBarFavourites() {
    FakeStoreTheme {
        BottomNavigationBar(
            selectedDestination = AppDestination.Favourites,
            onTabTap = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewBottomNavigationBarProfile() {
    FakeStoreTheme {
        BottomNavigationBar(
            selectedDestination = AppDestination.Profile,
            onTabTap = {},
        )
    }
}
