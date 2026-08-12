package com.guidovezzoni.fakestore.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guidovezzoni.fakestore.core.analytics.AnalyticsClient
import com.guidovezzoni.fakestore.domain.model.Product
import com.guidovezzoni.fakestore.domain.usecase.GetFavouriteIdsUseCase
import com.guidovezzoni.fakestore.domain.usecase.GetProductsUseCase
import com.guidovezzoni.fakestore.domain.usecase.ToggleFavouriteUseCase
import com.guidovezzoni.fakestore.ui.effect.FavouritesUiEffect
import com.guidovezzoni.fakestore.ui.intent.FavouritesUiIntent
import com.guidovezzoni.fakestore.ui.state.FavouritesUiState
import com.guidovezzoni.fakestore.ui.util.mapToProductListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val getFavouriteIdsUseCase: GetFavouriteIdsUseCase,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase,
    private val analyticsClient: AnalyticsClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FavouritesUiState>(FavouritesUiState.Loading)
    val uiState: StateFlow<FavouritesUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<FavouritesUiEffect>()
    val uiEffect: SharedFlow<FavouritesUiEffect> = _uiEffect.asSharedFlow()

    private val rawProducts = MutableStateFlow<List<Product>?>(null)

    init {
        viewModelScope.launch {
            combine(rawProducts.filterNotNull(), getFavouriteIdsUseCase()) { products, favouriteIds ->
                products
                    .filter { it.id in favouriteIds }
                    .map { product ->
                        mapToProductListItem(
                            product = product,
                            locale = Locale.getDefault(),
                            isFavourite = true,
                        )
                    }
            }.collect { items ->
                _uiState.value = FavouritesUiState.Content(items)
            }
        }
    }

    fun onIntent(intent: FavouritesUiIntent) {
        when (intent) {
            is FavouritesUiIntent.LoadFavourites -> loadFavourites()
            is FavouritesUiIntent.TrackScreenViewed -> trackScreenViewed()
            is FavouritesUiIntent.ToggleFavourite -> toggleFavourite(intent.productId)
        }
    }

    private fun trackScreenViewed() {
        viewModelScope.launch {
            val content = uiState.filterIsInstance<FavouritesUiState.Content>().first()
            analyticsClient.logEvent(
                name = EVENT_FAVOURITES_SCREEN_VIEWED,
                params = mapOf(PARAM_FAVOURITE_COUNT to content.products.size),
            )
        }
    }

    private fun loadFavourites() {
        if (_uiState.value is FavouritesUiState.Content) return
        _uiState.value = FavouritesUiState.Loading
        viewModelScope.launch {
            getProductsUseCase().collect { result ->
                result
                    .onSuccess { products ->
                        rawProducts.value = products
                    }
                    .onFailure {
                        _uiState.value = FavouritesUiState.Error
                    }
            }
        }
    }

    private fun toggleFavourite(productId: Int) {
        val currentContent = _uiState.value as? FavouritesUiState.Content ?: return
        val preToggleProducts = currentContent.products
        val updatedProducts = preToggleProducts.filter { it.id != productId }
        _uiState.value = FavouritesUiState.Content(updatedProducts)
        viewModelScope.launch {
            val result = toggleFavouriteUseCase(productId, SHOULD_BE_FAVOURITE)
            if (result.isSuccess) {
                analyticsClient.logEvent(name = EVENT_FAVOURITE_REMOVED, params = mapOf(PARAM_PRODUCT_ID to productId))
            } else {
                _uiState.value = FavouritesUiState.Content(preToggleProducts)
                _uiEffect.emit(FavouritesUiEffect.ShowFavouriteToggleError)
            }
        }
    }

    private companion object {
        const val EVENT_FAVOURITES_SCREEN_VIEWED = "favourites_screen_viewed"
        const val PARAM_FAVOURITE_COUNT = "favourite_count"
        const val EVENT_FAVOURITE_REMOVED = "favourite_removed"
        const val PARAM_PRODUCT_ID = "product_id"
        const val SHOULD_BE_FAVOURITE = false
    }
}
