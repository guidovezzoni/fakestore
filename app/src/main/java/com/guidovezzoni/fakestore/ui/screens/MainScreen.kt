package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.guidovezzoni.fakestore.ui.effect.MainUiEffect
import com.guidovezzoni.fakestore.ui.intent.MainUiIntent
import com.guidovezzoni.fakestore.ui.navigation.AppDestination
import com.guidovezzoni.fakestore.ui.state.MainUiState
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme
import com.guidovezzoni.fakestore.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Suppress("UnusedParameter")
@Composable
fun MainScreen(
    uiState: MainUiState,
    onIntent: (MainUiIntent) -> Unit,
    uiEffect: Flow<MainUiEffect>,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedDestination: AppDestination = when (navBackStackEntry?.destination?.route) {
        AppDestination.Favourites::class.qualifiedName -> AppDestination.Favourites
        AppDestination.Profile::class.qualifiedName -> AppDestination.Profile
        else -> AppDestination.Products
    }

    LaunchedEffect(navController) {
        uiEffect.collect { effect ->
            when (effect) {
                is MainUiEffect.NavigateToTab -> navController.navigate(effect.destination) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomNavigationBar(
                selectedDestination = selectedDestination,
                onTabTap = { onIntent(MainUiIntent.TabTapped(it)) },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Products,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable<AppDestination.Products> { ProductListScreen() }
            composable<AppDestination.Favourites> { FavouritesScreen() }
            composable<AppDestination.Profile> { ProfileScreen() }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MainScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        uiEffect = viewModel.uiEffect,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewMainScreenProductsSelected() {
    FakeStoreTheme {
        MainScreen(
            uiState = MainUiState(selectedDestination = AppDestination.Products),
            onIntent = {},
            uiEffect = emptyFlow(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMainScreenFavouritesSelected() {
    FakeStoreTheme {
        MainScreen(
            uiState = MainUiState(selectedDestination = AppDestination.Favourites),
            onIntent = {},
            uiEffect = emptyFlow(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewMainScreenProfileSelected() {
    FakeStoreTheme {
        MainScreen(
            uiState = MainUiState(selectedDestination = AppDestination.Profile),
            onIntent = {},
            uiEffect = emptyFlow(),
        )
    }
}
