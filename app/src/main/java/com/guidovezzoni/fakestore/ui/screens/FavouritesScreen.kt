package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guidovezzoni.fakestore.R
import com.guidovezzoni.fakestore.ui.effect.FavouritesUiEffect
import com.guidovezzoni.fakestore.ui.intent.FavouritesUiIntent
import com.guidovezzoni.fakestore.ui.state.FavouritesUiState
import com.guidovezzoni.fakestore.ui.state.ProductListItem
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme
import com.guidovezzoni.fakestore.ui.viewmodel.FavouritesViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

const val FAVOURITES_SCREEN_ROOT_TEST_TAG = "favourites_screen"
const val FAVOURITES_LOADING_INDICATOR_TEST_TAG = "favourites_loading_indicator"
const val FAVOURITES_EMPTY_MESSAGE_TEST_TAG = "favourites_empty_message"

@Composable
fun FavouritesScreen(
    uiState: FavouritesUiState,
    onIntent: (FavouritesUiIntent) -> Unit,
    modifier: Modifier = Modifier,
    uiEffect: Flow<FavouritesUiEffect> = emptyFlow(),
) {
    val currentOnIntent by rememberUpdatedState(onIntent)
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = stringResource(R.string.favourite_toggle_error_message)

    LaunchedEffect(Unit) {
        currentOnIntent(FavouritesUiIntent.LoadFavourites)
        currentOnIntent(FavouritesUiIntent.TrackScreenViewed)
    }

    LaunchedEffect(snackbarHostState) {
        uiEffect.collect { effect ->
            when (effect) {
                is FavouritesUiEffect.ShowFavouriteToggleError -> snackbarHostState.showSnackbar(errorMessage)
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag(FAVOURITES_SCREEN_ROOT_TEST_TAG),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (uiState) {
            FavouritesUiState.Loading -> FavouritesLoadingContent(
                modifier = Modifier.padding(innerPadding),
            )
            is FavouritesUiState.Content -> if (uiState.products.isEmpty()) {
                FavouritesEmptyContent(modifier = Modifier.padding(innerPadding))
            } else {
                LazyColumn(modifier = Modifier.padding(innerPadding)) {
                    items(uiState.products, key = { it.id }) { item ->
                        ProductListItemCard(
                            item = item,
                            onToggleFavourite = { productId ->
                                currentOnIntent(FavouritesUiIntent.ToggleFavourite(productId))
                            },
                        )
                    }
                }
            }
            FavouritesUiState.Error -> FavouritesErrorContent(
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun FavouritesLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.testTag(FAVOURITES_LOADING_INDICATOR_TEST_TAG))
    }
}

@Composable
private fun FavouritesEmptyContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .testTag(FAVOURITES_EMPTY_MESSAGE_TEST_TAG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.favourites_empty_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.favourites_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FavouritesErrorContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(R.string.product_list_error_message))
    }
}

@Composable
fun FavouritesScreen(
    modifier: Modifier = Modifier,
    viewModel: FavouritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FavouritesScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
        uiEffect = viewModel.uiEffect,
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewFavouritesScreenLoading() {
    FakeStoreTheme {
        FavouritesScreen(
            uiState = FavouritesUiState.Loading,
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFavouritesScreen() {
    FakeStoreTheme {
        FavouritesScreen(
            uiState = FavouritesUiState.Content(
                products = listOf(
                    ProductListItem(
                        id = FIRST_PREVIEW_PRODUCT_ID,
                        imageUrl = PREVIEW_IMAGE_URL,
                        title = FIRST_PREVIEW_TITLE,
                        formattedPrice = PREVIEW_FORMATTED_PRICE,
                        formattedRatingScore = PREVIEW_FORMATTED_RATING_SCORE,
                        isFavourite = true,
                    ),
                    ProductListItem(
                        id = SECOND_PREVIEW_PRODUCT_ID,
                        imageUrl = PREVIEW_IMAGE_URL,
                        title = SECOND_PREVIEW_TITLE,
                        formattedPrice = PREVIEW_FORMATTED_PRICE,
                        formattedRatingScore = PREVIEW_FORMATTED_RATING_SCORE,
                        isFavourite = true,
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFavouritesScreenEmpty() {
    FakeStoreTheme {
        FavouritesScreen(
            uiState = FavouritesUiState.Content(products = emptyList()),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFavouritesScreenError() {
    FakeStoreTheme {
        FavouritesScreen(
            uiState = FavouritesUiState.Error,
            onIntent = {},
        )
    }
}

private const val FIRST_PREVIEW_PRODUCT_ID = 1
private const val SECOND_PREVIEW_PRODUCT_ID = 2
private const val FIRST_PREVIEW_TITLE = "Fjallraven - Foldsack No. 1 Backpack, Fits 15 Laptops"
private const val SECOND_PREVIEW_TITLE = "Mens Casual Premium Slim Fit T-Shirts"
private const val PREVIEW_IMAGE_URL = "https://fakestoreapi.com/img/71YXzeOuslL._AC_UY879_.jpg"
private const val PREVIEW_FORMATTED_PRICE = "$109.95"
private const val PREVIEW_FORMATTED_RATING_SCORE = "4.1"
