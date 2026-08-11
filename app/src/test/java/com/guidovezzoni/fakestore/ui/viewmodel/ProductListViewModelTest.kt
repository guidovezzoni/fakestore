package com.guidovezzoni.fakestore.ui.viewmodel

import com.guidovezzoni.fakestore.domain.model.Product
import com.guidovezzoni.fakestore.domain.model.Rating
import com.guidovezzoni.fakestore.domain.usecase.GetProductsUseCase
import com.guidovezzoni.fakestore.ui.intent.ProductListUiIntent
import com.guidovezzoni.fakestore.ui.state.ProductListUiState
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

    // Task 2.1 — replaces the existing initial-state test
    @Test
    fun `GIVEN a newly constructed ProductListViewModel WHEN no intent has been dispatched THEN uiState value is ProductListUiState Loading`() {
        val expected = ProductListUiState.Loading

        val viewModel = createViewModel()
        val result = viewModel.uiState.value

        assertEquals(expected, result)
    }

    // Task 2.3 — updates the existing mapped-products test to the sealed type
    @Test
    fun `GIVEN a mocked GetProductsUseCase returning successful products WHEN LoadProducts is dispatched THEN uiState is Content with one mapped item per product in order`() = runTest {
        val products = listOf(
            createProduct(id = FIRST_PRODUCT_ID, title = FIRST_PRODUCT_TITLE),
            createProduct(id = SECOND_PRODUCT_ID, title = SECOND_PRODUCT_TITLE),
        )
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(products))
        val viewModel = createViewModel(getProductsUseCase)

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        val content = viewModel.uiState.value as ProductListUiState.Content

        val expectedSize = products.size
        assertEquals(expectedSize, content.products.size)
        assertEquals(products[0].id, content.products[0].id)
        assertEquals(products[1].id, content.products[1].id)
    }

    // Task 2.4 — empty list produces Content(products = emptyList())
    @Test
    fun `GIVEN a mocked GetProductsUseCase returning an empty list WHEN LoadProducts is dispatched THEN uiState equals Content with emptyList`() = runTest {
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(emptyList()))
        val viewModel = createViewModel(getProductsUseCase)

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        val result = viewModel.uiState.value

        val expected = ProductListUiState.Content(products = emptyList())
        assertEquals(expected, result)
    }

    // Task 2.5 — failure maps to Error with no technical detail in state
    @Test
    fun `GIVEN a mocked GetProductsUseCase returning failure WHEN LoadProducts is dispatched THEN uiState is ProductListUiState Error`() = runTest {
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(
            Result.failure(IllegalStateException("com.example.TechnicalException: internal detail"))
        )
        val viewModel = createViewModel(getProductsUseCase)

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        val result = viewModel.uiState.value

        val expected = ProductListUiState.Error
        assertEquals(expected, result)
    }

    // Task 2.7 — updates the existing dedup test to the sealed type
    @Test
    fun `GIVEN a mocked GetProductsUseCase returning a fixed product list WHEN LoadProducts is dispatched twice THEN uiState is Content with the mapped list and no duplicated entries`() = runTest {
        val products = listOf(createProduct())
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(products))
        val viewModel = createViewModel(getProductsUseCase)

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        val content = viewModel.uiState.value as ProductListUiState.Content

        val expectedSize = 1
        assertEquals(expectedSize, content.products.size)
        assertEquals(products[0].id, content.products[0].id)
    }

    // Task 2.8 — updates the existing formatting test to the sealed type
    @Test
    fun `GIVEN a mocked GetProductsUseCase returning products with known price and rating WHEN LoadProducts is dispatched THEN formattedPrice and formattedRatingScore match ProductListFormatter output`() = runTest {
        val product = createProduct(price = PRODUCT_PRICE, ratingScore = PRODUCT_RATING_SCORE)
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(listOf(product)))
        val viewModel = createViewModel(getProductsUseCase)
        val expectedFormattedPrice = formatPrice(product.price, Locale.getDefault())
        val expectedFormattedRatingScore = formatRatingScore(product.rating.score, Locale.getDefault())

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        val content = viewModel.uiState.value as ProductListUiState.Content
        val result = content.products.first()

        assertEquals(expectedFormattedPrice, result.formattedPrice)
        assertEquals(expectedFormattedRatingScore, result.formattedRatingScore)
    }

    // Task 2.9 — retry after failure transitions to Content on second success
    @Test
    fun `GIVEN a mocked GetProductsUseCase returning failure then success WHEN LoadProducts then RetryClicked are dispatched THEN uiState transitions to Content with the mapped products`() = runTest {
        val products = listOf(createProduct())
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returnsMany listOf(
            flowOf(Result.failure(IllegalStateException("error"))),
            flowOf(Result.success(products)),
        )
        val viewModel = createViewModel(getProductsUseCase)

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        viewModel.onIntent(ProductListUiIntent.RetryClicked)
        val content = viewModel.uiState.value as ProductListUiState.Content

        val expectedSize = 1
        assertEquals(expectedSize, content.products.size)
        assertEquals(products[0].id, content.products[0].id)
    }

    // Task 2.10 — retry from Error with continued failure stays in Error
    @Test
    fun `GIVEN a mocked GetProductsUseCase returning failure WHEN RetryClicked is dispatched from an Error state THEN uiState remains Error`() = runTest {
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.failure(IllegalStateException("error")))
        val viewModel = createViewModel(getProductsUseCase)

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        viewModel.onIntent(ProductListUiIntent.RetryClicked)
        val result = viewModel.uiState.value

        val expected = ProductListUiState.Error
        assertEquals(expected, result)
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
