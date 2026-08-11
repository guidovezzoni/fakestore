package com.guidovezzoni.fakestore.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guidovezzoni.fakestore.core.analytics.AnalyticsClient
import com.guidovezzoni.fakestore.domain.usecase.GetProductsUseCase
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
import kotlinx.coroutines.launch

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val analyticsClient: AnalyticsClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProductListUiEffect>()
    val uiEffect: SharedFlow<ProductListUiEffect> = _uiEffect.asSharedFlow()

    fun onIntent(intent: ProductListUiIntent) {
        when (intent) {
            is ProductListUiIntent.LoadProducts -> loadProductsOrTrack()
            is ProductListUiIntent.RetryClicked -> loadProducts()
        }
    }

    private fun loadProductsOrTrack() {
        if (_uiState.value is ProductListUiState.Content) {
            analyticsClient.logEvent(EVENT_PRODUCT_LIST_VIEWED)
            return
        }
        loadProducts()
    }

    private companion object {
        const val EVENT_PRODUCT_LIST_VIEWED = "product_list_viewed"
    }

    private fun loadProducts() {
        val locale = Locale.getDefault()
        _uiState.value = ProductListUiState.Loading
        viewModelScope.launch {
            getProductsUseCase().collect { result ->
                result
                    .onSuccess { products ->
                        _uiState.value = ProductListUiState.Content(
                            products.map { mapToProductListItem(it, locale) }
                        )
                        analyticsClient.logEvent(EVENT_PRODUCT_LIST_VIEWED)
                    }
                    .onFailure {
                        _uiState.value = ProductListUiState.Error
                    }
            }
        }
    }
}
