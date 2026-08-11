package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guidovezzoni.fakestore.R
import com.guidovezzoni.fakestore.ui.intent.ProductListUiIntent
import com.guidovezzoni.fakestore.ui.state.ProductListItem
import com.guidovezzoni.fakestore.ui.state.ProductListUiState
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme
import com.guidovezzoni.fakestore.ui.viewmodel.ProductListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    uiState: ProductListUiState,
    onIntent: (ProductListUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnIntent by rememberUpdatedState(onIntent)
    LaunchedEffect(Unit) {
        currentOnIntent(ProductListUiIntent.LoadProducts)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.product_list_screen_title)) })
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(uiState.products, key = { it.id }) { item ->
                ProductListItemCard(item = item)
            }
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
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewProductListScreen() {
    FakeStoreTheme {
        ProductListScreen(
            uiState = ProductListUiState(
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

private const val FIRST_PREVIEW_PRODUCT_ID = 1
private const val SECOND_PREVIEW_PRODUCT_ID = 2
private const val FIRST_PREVIEW_TITLE = "Fjallraven - Foldsack No. 1 Backpack, Fits 15 Laptops"
private const val SECOND_PREVIEW_TITLE = "Mens Casual Premium Slim Fit T-Shirts"
private const val PREVIEW_IMAGE_URL = "https://fakestoreapi.com/img/71YXzeOuslL._AC_UY879_.jpg"
private const val PREVIEW_FORMATTED_PRICE = "$109.95"
private const val PREVIEW_FORMATTED_RATING_SCORE = "4.1"
