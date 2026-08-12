package com.guidovezzoni.fakestore.ui.viewmodel

import com.guidovezzoni.fakestore.core.analytics.AnalyticsClient
import com.guidovezzoni.fakestore.domain.model.Product
import com.guidovezzoni.fakestore.domain.model.Rating
import com.guidovezzoni.fakestore.domain.usecase.GetFavouriteIdsUseCase
import com.guidovezzoni.fakestore.domain.usecase.GetProductsUseCase
import com.guidovezzoni.fakestore.domain.usecase.ToggleFavouriteUseCase
import com.guidovezzoni.fakestore.ui.effect.ProductListUiEffect
import com.guidovezzoni.fakestore.ui.intent.ProductListUiIntent
import com.guidovezzoni.fakestore.ui.state.ProductListUiState
import com.guidovezzoni.fakestore.ui.util.formatPrice
import com.guidovezzoni.fakestore.ui.util.formatRatingScore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
        getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk<GetFavouriteIdsUseCase>().also { mock ->
            every { mock() } returns flowOf(emptySet())
        },
        toggleFavouriteUseCase: ToggleFavouriteUseCase = mockk<ToggleFavouriteUseCase>().also { mock ->
            coEvery { mock(any(), any()) } returns Result.success(Unit)
        },
        analyticsClient: AnalyticsClient = mockk(relaxed = true),
    ) = ProductListViewModel(getProductsUseCase, getFavouriteIdsUseCase, toggleFavouriteUseCase, analyticsClient)

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

    // Task 5.2 — success with non-empty list fires product_list_viewed once with empty params
    @Test
    fun givenSuccessNonEmptyProducts_whenLoadProductsDispatched_thenLogEventCalledOnceWithProductListViewed() = runTest {
        val products = listOf(createProduct())
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(products))
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(getProductsUseCase = getProductsUseCase, analyticsClient = analyticsClient)

        viewModel.onIntent(ProductListUiIntent.LoadProducts)

        verify(exactly = 1) { analyticsClient.logEvent(name = "product_list_viewed", params = emptyMap()) }
    }

    // Task 5.3 — success with empty list still fires product_list_viewed once
    @Test
    fun givenSuccessEmptyProducts_whenLoadProductsDispatched_thenLogEventCalledOnceWithProductListViewed() = runTest {
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(emptyList()))
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(getProductsUseCase = getProductsUseCase, analyticsClient = analyticsClient)

        viewModel.onIntent(ProductListUiIntent.LoadProducts)

        verify(exactly = 1) { analyticsClient.logEvent(name = "product_list_viewed", params = emptyMap()) }
    }

    // Task 5.4 — failure never fires product_list_viewed
    @Test
    fun givenFailure_whenLoadProductsDispatched_thenLogEventNeverCalled() = runTest {
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.failure(IllegalStateException("error")))
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(getProductsUseCase = getProductsUseCase, analyticsClient = analyticsClient)

        viewModel.onIntent(ProductListUiIntent.LoadProducts)

        verify(exactly = 0) { analyticsClient.logEvent(any()) }
    }

    // Task 5.5 — success dispatched twice fires product_list_viewed exactly twice
    @Test
    fun givenSuccessProducts_whenLoadProductsDispatchedTwice_thenLogEventCalledTwice() = runTest {
        val products = listOf(createProduct())
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(products))
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(getProductsUseCase = getProductsUseCase, analyticsClient = analyticsClient)

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        viewModel.onIntent(ProductListUiIntent.LoadProducts)

        verify(exactly = 2) { analyticsClient.logEvent(name = "product_list_viewed", params = emptyMap()) }
    }

    // Guard — already in Content: LoadProducts logs analytics but does not re-fetch from network
    @Test
    fun givenAlreadyInContentState_whenLoadProductsDispatched_thenLogEventCalledAndNetworkNotCalledAgain() = runTest {
        val products = listOf(createProduct())
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(products))
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(getProductsUseCase = getProductsUseCase, analyticsClient = analyticsClient)

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        viewModel.onIntent(ProductListUiIntent.LoadProducts)

        val expected = 1
        verify(exactly = expected) { getProductsUseCase() }
        verify(exactly = 2) { analyticsClient.logEvent(name = "product_list_viewed", params = emptyMap()) }
    }

    // Task 5.6 — failure then success via RetryClicked fires product_list_viewed exactly once
    @Test
    fun givenFailureThenSuccess_whenLoadProductsThenRetryClicked_thenLogEventCalledOnce() = runTest {
        val products = listOf(createProduct())
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returnsMany listOf(
            flowOf(Result.failure(IllegalStateException("error"))),
            flowOf(Result.success(products)),
        )
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(getProductsUseCase = getProductsUseCase, analyticsClient = analyticsClient)

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        viewModel.onIntent(ProductListUiIntent.RetryClicked)

        verify(exactly = 1) { analyticsClient.logEvent(name = "product_list_viewed", params = emptyMap()) }
    }

    // Task 9.1 — combine: favourite ids applied to products on load
    @Test
    fun `GIVEN products with ids 1 and 2 and GetFavouriteIdsUseCase emitting setOf(2) WHEN LoadProducts is dispatched THEN item with id 2 has isFavourite true and item with id 1 has isFavourite false`() = runTest {
        val products = listOf(
            createProduct(id = FIRST_PRODUCT_ID),
            createProduct(id = SECOND_PRODUCT_ID),
        )
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(products))
        val getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk()
        every { getFavouriteIdsUseCase() } returns flowOf(setOf(SECOND_PRODUCT_ID))
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            getFavouriteIdsUseCase = getFavouriteIdsUseCase,
        )

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        val content = viewModel.uiState.value as ProductListUiState.Content

        val expectedFirstIsFavourite = false
        val expectedSecondIsFavourite = true
        assertEquals(expectedFirstIsFavourite, content.products.first { it.id == FIRST_PRODUCT_ID }.isFavourite)
        assertEquals(expectedSecondIsFavourite, content.products.first { it.id == SECOND_PRODUCT_ID }.isFavourite)
    }

    // Task 9.2 — reactive: new favourites emission updates uiState without new intent
    @Test
    fun `GIVEN LoadProducts has succeeded WHEN GetFavouriteIdsUseCase emits a new set including a previously-unfavourited product THEN uiState item isFavourite becomes true`() = runTest {
        val products = listOf(
            createProduct(id = FIRST_PRODUCT_ID),
            createProduct(id = SECOND_PRODUCT_ID),
        )
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(products))
        val favouriteIdsFlow = MutableStateFlow<Set<Int>>(emptySet())
        val getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk()
        every { getFavouriteIdsUseCase() } returns favouriteIdsFlow
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            getFavouriteIdsUseCase = getFavouriteIdsUseCase,
        )

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        favouriteIdsFlow.value = setOf(FIRST_PRODUCT_ID)
        val content = viewModel.uiState.value as ProductListUiState.Content

        val expectedIsFavourite = true
        assertEquals(expectedIsFavourite, content.products.first { it.id == FIRST_PRODUCT_ID }.isFavourite)
    }

    // Task 9.3 — optimistic update: isFavourite flipped before write completes
    @Test
    fun `GIVEN Content with product id 7 and isFavourite false WHEN ToggleFavourite is dispatched before write completes THEN uiState item id 7 has isFavourite true`() = runTest {
        val product = createProduct(id = TOGGLE_PRODUCT_ID)
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(listOf(product)))
        val deferred = CompletableDeferred<Result<Unit>>()
        val toggleFavouriteUseCase: ToggleFavouriteUseCase = mockk()
        coEvery { toggleFavouriteUseCase(TOGGLE_PRODUCT_ID, true) } coAnswers { deferred.await() }
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            toggleFavouriteUseCase = toggleFavouriteUseCase,
        )

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        viewModel.onIntent(ProductListUiIntent.ToggleFavourite(productId = TOGGLE_PRODUCT_ID))
        val content = viewModel.uiState.value as ProductListUiState.Content

        val expectedIsFavourite = true
        assertEquals(expectedIsFavourite, content.products.first { it.id == TOGGLE_PRODUCT_ID }.isFavourite)

        deferred.cancel()
    }

    // Task 9.4 — analytics: favourite_added logged on successful add
    @Test
    fun givenProductId7IsFavouriteFalseAndSuccessfulToggle_whenToggleFavouriteDispatched_thenFavouriteAddedLoggedOnce() = runTest {
        val product = createProduct(id = TOGGLE_PRODUCT_ID)
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(listOf(product)))
        val toggleFavouriteUseCase: ToggleFavouriteUseCase = mockk()
        coEvery { toggleFavouriteUseCase(TOGGLE_PRODUCT_ID, true) } returns Result.success(Unit)
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            toggleFavouriteUseCase = toggleFavouriteUseCase,
            analyticsClient = analyticsClient,
        )

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        viewModel.onIntent(ProductListUiIntent.ToggleFavourite(productId = TOGGLE_PRODUCT_ID))

        verify(exactly = 1) {
            analyticsClient.logEvent(name = "favourite_added", params = mapOf("product_id" to TOGGLE_PRODUCT_ID))
        }
    }

    // Task 9.5 — analytics: favourite_removed logged on successful remove
    @Test
    fun givenProductId7IsFavouriteTrueAndSuccessfulToggle_whenToggleFavouriteDispatched_thenFavouriteRemovedLoggedOnce() = runTest {
        val product = createProduct(id = TOGGLE_PRODUCT_ID)
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(listOf(product)))
        val getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk()
        every { getFavouriteIdsUseCase() } returns flowOf(setOf(TOGGLE_PRODUCT_ID))
        val toggleFavouriteUseCase: ToggleFavouriteUseCase = mockk()
        coEvery { toggleFavouriteUseCase(TOGGLE_PRODUCT_ID, false) } returns Result.success(Unit)
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            getFavouriteIdsUseCase = getFavouriteIdsUseCase,
            toggleFavouriteUseCase = toggleFavouriteUseCase,
            analyticsClient = analyticsClient,
        )

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        viewModel.onIntent(ProductListUiIntent.ToggleFavourite(productId = TOGGLE_PRODUCT_ID))

        verify(exactly = 1) {
            analyticsClient.logEvent(name = "favourite_removed", params = mapOf("product_id" to TOGGLE_PRODUCT_ID))
        }
    }

    // Task 9.6 — failure: state reverts, effect emitted, no analytics
    @Test
    fun givenProductId7IsFavouriteFalseAndFailedToggle_whenToggleFavouriteDispatched_thenStateRevertsAndEffectEmittedAndNoAnalytics() = runTest {
        val product = createProduct(id = TOGGLE_PRODUCT_ID)
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(listOf(product)))
        val toggleFavouriteUseCase: ToggleFavouriteUseCase = mockk()
        coEvery { toggleFavouriteUseCase(TOGGLE_PRODUCT_ID, true) } returns Result.failure(RuntimeException("db error"))
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            toggleFavouriteUseCase = toggleFavouriteUseCase,
            analyticsClient = analyticsClient,
        )

        val effectDeferred = async(testDispatcher) { viewModel.uiEffect.first() }

        viewModel.onIntent(ProductListUiIntent.LoadProducts)
        viewModel.onIntent(ProductListUiIntent.ToggleFavourite(productId = TOGGLE_PRODUCT_ID))

        val content = viewModel.uiState.value as ProductListUiState.Content
        val expectedIsFavourite = false
        assertEquals(expectedIsFavourite, content.products.first { it.id == TOGGLE_PRODUCT_ID }.isFavourite)

        val expectedEffect = ProductListUiEffect.ShowFavouriteToggleError
        assertEquals(expectedEffect, effectDeferred.await())

        verify(exactly = 0) { analyticsClient.logEvent(name = "favourite_added", params = any()) }
        verify(exactly = 0) { analyticsClient.logEvent(name = "favourite_removed", params = any()) }
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
        const val TOGGLE_PRODUCT_ID = 7
    }
}
