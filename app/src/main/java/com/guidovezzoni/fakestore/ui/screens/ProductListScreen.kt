package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guidovezzoni.fakestore.R
import com.guidovezzoni.fakestore.ui.effect.ProductListUiEffect
import com.guidovezzoni.fakestore.ui.intent.ProductListUiIntent
import com.guidovezzoni.fakestore.ui.state.ProductListItem
import com.guidovezzoni.fakestore.ui.state.ProductListUiState
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme
import com.guidovezzoni.fakestore.ui.viewmodel.ProductListViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

const val PRODUCT_LIST_LOADING_INDICATOR_TEST_TAG = "product_list_loading_indicator"
const val PRODUCT_LIST_ERROR_CONTAINER_TEST_TAG = "product_list_error_container"
const val PRODUCT_LIST_RETRY_BUTTON_TEST_TAG = "product_list_retry_button"
const val PRODUCT_LIST_EMPTY_MESSAGE_TEST_TAG = "product_list_empty_message"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    uiState: ProductListUiState,
    onIntent: (ProductListUiIntent) -> Unit,
    modifier: Modifier = Modifier,
    uiEffect: Flow<ProductListUiEffect> = emptyFlow(),
) {
    val currentOnIntent by rememberUpdatedState(onIntent)
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = stringResource(R.string.favourite_toggle_error_message)

    LaunchedEffect(Unit) {
        currentOnIntent(ProductListUiIntent.LoadProducts)
    }

    LaunchedEffect(snackbarHostState) {
        uiEffect.collect { effect ->
            when (effect) {
                is ProductListUiEffect.ShowFavouriteToggleError -> snackbarHostState.showSnackbar(errorMessage)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.product_list_screen_title)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (uiState) {
            ProductListUiState.Loading -> ProductListLoadingContent(
                modifier = Modifier.padding(innerPadding),
            )
            is ProductListUiState.Content -> if (uiState.products.isEmpty()) {
                ProductListEmptyContent(modifier = Modifier.padding(innerPadding))
            } else {
                LazyColumn(modifier = Modifier.padding(innerPadding)) {
                    items(uiState.products, key = { it.id }) { item ->
                        ProductListItemCard(
                            item = item,
                            onToggleFavourite = { currentOnIntent(ProductListUiIntent.ToggleFavourite(it)) },
                        )
                    }
                }
            }
            ProductListUiState.Error -> ProductListErrorContent(
                onRetry = { currentOnIntent(ProductListUiIntent.RetryClicked) },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ProductListLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.testTag(PRODUCT_LIST_LOADING_INDICATOR_TEST_TAG))
    }
}

@Composable
private fun ProductListEmptyContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.product_list_empty_message),
            modifier = Modifier.testTag(PRODUCT_LIST_EMPTY_MESSAGE_TEST_TAG),
        )
    }
}

@Composable
private fun ProductListErrorContent(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().testTag(PRODUCT_LIST_ERROR_CONTAINER_TEST_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = stringResource(R.string.product_list_error_message))
        Button(
            onClick = onRetry,
            modifier = Modifier.testTag(PRODUCT_LIST_RETRY_BUTTON_TEST_TAG),
        ) {
            Text(text = stringResource(R.string.product_list_retry_button))
        }
    }
}

@Composable
fun ProductListScreen(
    modifier: Modifier = Modifier,
    viewModel: ProductListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProductListScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
        uiEffect = viewModel.uiEffect,
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewProductListScreen() {
    FakeStoreTheme {
        ProductListScreen(
            uiState = ProductListUiState.Content(
                products = listOf(
                    ProductListItem(
                        id = FIRST_PREVIEW_PRODUCT_ID,
                        imageUrl = PREVIEW_IMAGE_URL,
                        title = FIRST_PREVIEW_TITLE,
                        formattedPrice = PREVIEW_FORMATTED_PRICE,
                        formattedRatingScore = PREVIEW_FORMATTED_RATING_SCORE,
                    ),
                    ProductListItem(
                        id = SECOND_PREVIEW_PRODUCT_ID,
                        imageUrl = PREVIEW_IMAGE_URL,
                        title = SECOND_PREVIEW_TITLE,
                        formattedPrice = PREVIEW_FORMATTED_PRICE,
                        formattedRatingScore = PREVIEW_FORMATTED_RATING_SCORE,
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewProductListScreenLoading() {
    FakeStoreTheme {
        ProductListScreen(
            uiState = ProductListUiState.Loading,
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewProductListScreenEmpty() {
    FakeStoreTheme {
        ProductListScreen(
            uiState = ProductListUiState.Content(products = emptyList()),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewProductListScreenError() {
    FakeStoreTheme {
        ProductListScreen(
            uiState = ProductListUiState.Error,
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
