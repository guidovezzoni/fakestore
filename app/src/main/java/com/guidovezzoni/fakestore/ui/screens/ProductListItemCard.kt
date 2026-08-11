package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.guidovezzoni.fakestore.R
import com.guidovezzoni.fakestore.ui.state.ProductListItem
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme

private val CARD_HORIZONTAL_PADDING = 16.dp
private val CARD_VERTICAL_PADDING = 8.dp
private val CARD_CONTENT_PADDING = 12.dp
private val PRODUCT_IMAGE_SIZE = 80.dp
private val CONTENT_START_PADDING = 12.dp
private val TEXT_TOP_PADDING = 4.dp

const val PRODUCT_LIST_ITEM_CARD_TEST_TAG_PREFIX = "product_list_item_card_"

@Composable
fun ProductListItemCard(
    item: ProductListItem,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CARD_HORIZONTAL_PADDING, vertical = CARD_VERTICAL_PADDING)
            .testTag(PRODUCT_LIST_ITEM_CARD_TEST_TAG_PREFIX + item.id),
    ) {
        Row(
            modifier = Modifier.padding(CARD_CONTENT_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = stringResource(R.string.product_image_content_description, item.title),
                placeholder = rememberVectorPainter(Icons.Default.Image),
                error = rememberVectorPainter(Icons.Default.Image),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(PRODUCT_IMAGE_SIZE),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = CONTENT_START_PADDING),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = item.formattedPrice,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = TEXT_TOP_PADDING),
                )
                Text(
                    text = item.formattedRatingScore,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = TEXT_TOP_PADDING),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewProductListItemCard() {
    FakeStoreTheme {
        ProductListItemCard(
            item = ProductListItem(
                id = PREVIEW_PRODUCT_ID,
                imageUrl = PREVIEW_IMAGE_URL,
                title = PREVIEW_TITLE,
                formattedPrice = PREVIEW_FORMATTED_PRICE,
                formattedRatingScore = PREVIEW_FORMATTED_RATING_SCORE,
            ),
        )
    }
}

private const val PREVIEW_PRODUCT_ID = 1
private const val PREVIEW_IMAGE_URL = "https://fakestoreapi.com/img/71YXzeOuslL._AC_UY879_.jpg"
private const val PREVIEW_TITLE = "Fjallraven - Foldsack No. 1 Backpack, Fits 15 Laptops"
private const val PREVIEW_FORMATTED_PRICE = "$109.95"
private const val PREVIEW_FORMATTED_RATING_SCORE = "4.1"
