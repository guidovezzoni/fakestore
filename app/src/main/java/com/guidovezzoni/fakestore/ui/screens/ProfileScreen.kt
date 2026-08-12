package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guidovezzoni.fakestore.R
import com.guidovezzoni.fakestore.ui.intent.ProfileUiIntent
import com.guidovezzoni.fakestore.ui.state.ProfileUiState
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme
import com.guidovezzoni.fakestore.ui.viewmodel.ProfileViewModel

const val PROFILE_LOADING_TEST_TAG = "profile_loading"
const val PROFILE_CONTENT_TEST_TAG = "profile_content"
const val PROFILE_ERROR_TEST_TAG = "profile_error"

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    modifier: Modifier = Modifier,
    onIntent: (ProfileUiIntent) -> Unit = {},
) {
    val currentOnIntent by rememberUpdatedState(onIntent)

    LaunchedEffect(Unit) {
        currentOnIntent(ProfileUiIntent.LoadProfile)
        currentOnIntent(ProfileUiIntent.TrackScreenViewed)
    }

    Scaffold(modifier = modifier) { innerPadding ->
        when (uiState) {
            ProfileUiState.Loading -> ProfileLoadingContent(
                modifier = Modifier.padding(innerPadding),
            )
            is ProfileUiState.Content -> ProfileContentScreen(
                uiState = uiState,
                modifier = Modifier.padding(innerPadding),
            )
            ProfileUiState.Error -> ProfileErrorContent(
                modifier = Modifier.padding(innerPadding),
                onRetry = { currentOnIntent(ProfileUiIntent.RetryClicked) },
            )
        }
    }
}

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        modifier = modifier,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun ProfileLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.testTag(PROFILE_LOADING_TEST_TAG))
    }
}

@Composable
private fun ProfileErrorContent(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(PROFILE_ERROR_TEST_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = stringResource(R.string.product_list_error_message))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.product_list_retry_button))
        }
    }
}

@Composable
private fun ProfileContentScreen(
    uiState: ProfileUiState.Content,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(PROFILE_CONTENT_TEST_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = uiState.fullName)
        Text(text = uiState.email)
        Text(text = uiState.favouriteCount.toString())
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewProfileScreenLoading() {
    FakeStoreTheme {
        ProfileScreen(
            uiState = ProfileUiState.Loading,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewProfileScreen() {
    FakeStoreTheme {
        ProfileScreen(
            uiState = ProfileUiState.Content(
                fullName = PREVIEW_FULL_NAME,
                email = PREVIEW_EMAIL,
                favouriteCount = PREVIEW_FAVOURITE_COUNT,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewProfileScreenError() {
    FakeStoreTheme {
        ProfileScreen(
            uiState = ProfileUiState.Error,
        )
    }
}

private const val PREVIEW_FULL_NAME = "John Doe"
private const val PREVIEW_EMAIL = "john@example.com"
private const val PREVIEW_FAVOURITE_COUNT = 3
