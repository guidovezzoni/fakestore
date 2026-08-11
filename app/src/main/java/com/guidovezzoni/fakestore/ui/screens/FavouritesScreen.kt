package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.guidovezzoni.fakestore.R
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme

@Composable
fun FavouritesScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(R.string.favourites_placeholder))
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFavouritesScreen() {
    FakeStoreTheme {
        FavouritesScreen()
    }
}
