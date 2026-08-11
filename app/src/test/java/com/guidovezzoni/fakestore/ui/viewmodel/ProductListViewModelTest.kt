package com.guidovezzoni.fakestore.ui.viewmodel

import com.guidovezzoni.fakestore.domain.model.Product
import com.guidovezzoni.fakestore.domain.model.Rating
import com.guidovezzoni.fakestore.domain.usecase.GetProductsUseCase
import com.guidovezzoni.fakestore.ui.intent.ProductListUiIntent
import com.guidovezzoni.fakestore.ui.state.ProductListItem
import com.guidovezzoni.fakestore.ui.util.formatPrice
import com.guidovezzoni.fakestore.ui.util.formatRatingScore
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        getProductsUseCase: GetProductsUseCase = mockk(),
    ) = ProductListViewModel(getProductsUseCase)

    private fun createProduct(
        id: Int = PRODUCT_ID,
        title: String = PRODUCT_TITLE,
        price: Double = PRODUCT_PRICE,
        ratingScore: Double = PRODUCT_RATING_SCORE,
    ) = Product(
        id = id,
        title = title,
        price = price,
        description = PRODUCT_DESCRIPTION,
        category = PRODUCT_CATEGORY,
        imageUrl = PRODUCT_IMAGE_URL,
        rating = Rating(score = ratingScore, count = PRODUCT_RATING_COUNT),
    )

    @Test
    fun `GIVEN a newly constructed ProductListViewModel WHEN no intent has been dispatched THEN uiState products equals emptyList`() {
        val expected = emptyList<ProductListItem>()

        val viewModel = createViewModel()
        val result = viewModel.uiState.value.products

        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN a mocked GetProductsUseCase returning successful products WHEN LoadProducts is dispatched THEN uiState products contains one mapped item per product in order`() = runTest {
        val products = listOf(
            createProduct(id = FIRST_PRODUCT_ID, title = FIRST_PRODUCT_TITLE),
            createProduct(id = SECOND_PRODUCT_ID, title = SECOND_PRODUCT_TITLE),
        )
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(products))
        val viewModel = createViewModel(getProductsUseCase)

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        val result = viewModel.uiState.value.products

        val expectedSize = products.size
        assertEquals(expectedSize, result.size)
        assertEquals(products[0].id, result[0].id)
        assertEquals(products[1].id, result[1].id)
    }

    @Test
    fun `GIVEN a mocked GetProductsUseCase returning a fixed product list WHEN LoadProducts is dispatched twice THEN uiState products still equals the mapped list with no duplicated entries`() = runTest {
        val products = listOf(createProduct())
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(products))
        val viewModel = createViewModel(getProductsUseCase)

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        val result = viewModel.uiState.value.products

        val expectedSize = 1
        assertEquals(expectedSize, result.size)
        assertEquals(products[0].id, result[0].id)
    }

    @Test
    fun `GIVEN a mocked GetProductsUseCase returning products with known price and rating WHEN LoadProducts is dispatched THEN formattedPrice and formattedRatingScore match ProductListFormatter output`() = runTest {
        val product = createProduct(price = PRODUCT_PRICE, ratingScore = PRODUCT_RATING_SCORE)
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(listOf(product)))
        val viewModel = createViewModel(getProductsUseCase)
        val expectedFormattedPrice = formatPrice(product.price, Locale.getDefault())
        val expectedFormattedRatingScore = formatRatingScore(product.rating.score, Locale.getDefault())

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        val result = viewModel.uiState.value.products.first()

        assertEquals(expectedFormattedPrice, result.formattedPrice)
        assertEquals(expectedFormattedRatingScore, result.formattedRatingScore)
    }

    private companion object {
        const val PRODUCT_ID = 1
        const val PRODUCT_TITLE = "Test Product"
        const val PRODUCT_PRICE = 109.95
        const val PRODUCT_DESCRIPTION = "A test product"
        const val PRODUCT_CATEGORY = "test"
        const val PRODUCT_IMAGE_URL = "https://example.com/image.png"
        const val PRODUCT_RATING_SCORE = 4.1
        const val PRODUCT_RATING_COUNT = 100
        const val FIRST_PRODUCT_ID = 1
        const val FIRST_PRODUCT_TITLE = "First"
        const val SECOND_PRODUCT_ID = 2
        const val SECOND_PRODUCT_TITLE = "Second"
    }
}
