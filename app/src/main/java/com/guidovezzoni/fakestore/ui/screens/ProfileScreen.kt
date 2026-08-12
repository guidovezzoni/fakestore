package com.guidovezzoni.fakestore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guidovezzoni.fakestore.R
import com.guidovezzoni.fakestore.ui.intent.ProfileUiIntent
import com.guidovezzoni.fakestore.ui.state.ProfileUiState
import com.guidovezzoni.fakestore.ui.theme.FakeStoreTheme
import com.guidovezzoni.fakestore.ui.viewmodel.ProfileViewModel

private val AVATAR_SIZE = 96.dp
private val HERO_VERTICAL_PADDING = 40.dp
private val HERO_HORIZONTAL_PADDING = 24.dp
private val AVATAR_NAME_SPACING = 16.dp
private val NAME_EMAIL_SPACING = 4.dp
private val CONTENT_PADDING = 16.dp
private val CONTENT_ITEM_SPACING = 12.dp
private val CARD_CONTENT_PADDING = 16.dp
private val CARD_ICON_SPACING = 12.dp
private val ICON_SIZE = 24.dp
private const val EMAIL_ALPHA = 0.7f

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
private fun ProfileHeroSection(
    uiState: ProfileUiState.Content,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(
                vertical = HERO_VERTICAL_PADDING,
                horizontal = HERO_HORIZONTAL_PADDING,
            ),
        ) {
            Box(
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.initials,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(modifier = Modifier.height(AVATAR_NAME_SPACING))
            Text(
                text = uiState.fullName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(NAME_EMAIL_SPACING))
            Text(
                text = uiState.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = EMAIL_ALPHA),
            )
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
            .testTag(PROFILE_CONTENT_TEST_TAG)
            .verticalScroll(rememberScrollState()),
    ) {
        ProfileHeroSection(uiState = uiState)

        Column(
            modifier = Modifier.padding(CONTENT_PADDING),
            verticalArrangement = Arrangement.spacedBy(CONTENT_ITEM_SPACING),
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(CARD_CONTENT_PADDING),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CARD_ICON_SPACING),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(ICON_SIZE),
                    )
                    Text(
                        text = stringResource(R.string.profile_favourite_count_label),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = uiState.favouriteCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
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
                initials = PREVIEW_INITIALS,
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
private const val PREVIEW_INITIALS = "JD"
