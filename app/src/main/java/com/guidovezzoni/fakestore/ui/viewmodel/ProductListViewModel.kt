package com.guidovezzoni.fakestore.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guidovezzoni.fakestore.core.analytics.AnalyticsClient
import com.guidovezzoni.fakestore.domain.model.Product
import com.guidovezzoni.fakestore.domain.usecase.GetFavouriteIdsUseCase
import com.guidovezzoni.fakestore.domain.usecase.GetProductsUseCase
import com.guidovezzoni.fakestore.domain.usecase.ToggleFavouriteUseCase
import com.guidovezzoni.fakestore.ui.effect.ProductListUiEffect
import com.guidovezzoni.fakestore.ui.intent.ProductListUiIntent
import com.guidovezzoni.fakestore.ui.state.ProductListUiState
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val getFavouriteIdsUseCase: GetFavouriteIdsUseCase,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase,
    private val analyticsClient: AnalyticsClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProductListUiEffect>()
    val uiEffect: SharedFlow<ProductListUiEffect> = _uiEffect.asSharedFlow()

    private val rawProducts = MutableStateFlow<List<Product>?>(null)

    init {
        viewModelScope.launch {
            combine(rawProducts.filterNotNull(), getFavouriteIdsUseCase()) { products, favouriteIds ->
                products.map { product ->
                    mapToProductListItem(
                        product = product,
                        locale = Locale.getDefault(),
                        isFavourite = favouriteIds.contains(product.id),
                    )
                }
            }.collect { items ->
                _uiState.value = ProductListUiState.Content(items)
            }
        }
    }

    fun onIntent(intent: ProductListUiIntent) {
        when (intent) {
            is ProductListUiIntent.LoadProducts -> loadProductsOrTrack()
            is ProductListUiIntent.RetryClicked -> loadProducts()
            is ProductListUiIntent.ToggleFavourite -> toggleFavourite(intent.productId)
        }
    }

    private fun loadProductsOrTrack() {
        if (_uiState.value is ProductListUiState.Content) {
            analyticsClient.logEvent(EVENT_PRODUCT_LIST_VIEWED)
            return
        }
        loadProducts()
    }

    private fun loadProducts() {
        _uiState.value = ProductListUiState.Loading
        viewModelScope.launch {
            getProductsUseCase().collect { result ->
                result
                    .onSuccess { products ->
                        rawProducts.value = products
                        analyticsClient.logEvent(EVENT_PRODUCT_LIST_VIEWED)
                    }
                    .onFailure {
                        _uiState.value = ProductListUiState.Error
                    }
            }
        }
    }

    private fun toggleFavourite(productId: Int) {
        val currentContent = _uiState.value as? ProductListUiState.Content ?: return
        val preToggleProducts = currentContent.products
        val item = preToggleProducts.firstOrNull { it.id == productId } ?: return
        val newIsFavourite = !item.isFavourite
        val updatedProducts = preToggleProducts.map { product ->
            if (product.id == productId) product.copy(isFavourite = newIsFavourite) else product
        }
        _uiState.value = ProductListUiState.Content(updatedProducts)
        viewModelScope.launch {
            val result = toggleFavouriteUseCase(productId, newIsFavourite)
            if (result.isSuccess) {
                val eventName = if (newIsFavourite) EVENT_FAVOURITE_ADDED else EVENT_FAVOURITE_REMOVED
                analyticsClient.logEvent(name = eventName, params = mapOf(PARAM_PRODUCT_ID to productId))
            } else {
                _uiState.value = ProductListUiState.Content(preToggleProducts)
                _uiEffect.emit(ProductListUiEffect.ShowFavouriteToggleError)
            }
        }
    }

    private companion object {
        const val EVENT_PRODUCT_LIST_VIEWED = "product_list_viewed"
        const val EVENT_FAVOURITE_ADDED = "favourite_added"
        const val EVENT_FAVOURITE_REMOVED = "favourite_removed"
        const val PARAM_PRODUCT_ID = "product_id"
    }
}
