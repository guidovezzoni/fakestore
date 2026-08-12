package com.guidovezzoni.fakestore.ui.viewmodel

import com.guidovezzoni.fakestore.core.analytics.AnalyticsClient
import com.guidovezzoni.fakestore.domain.model.Product
import com.guidovezzoni.fakestore.domain.model.Rating
import com.guidovezzoni.fakestore.domain.usecase.GetFavouriteIdsUseCase
import com.guidovezzoni.fakestore.domain.usecase.GetProductsUseCase
import com.guidovezzoni.fakestore.domain.usecase.ToggleFavouriteUseCase
import com.guidovezzoni.fakestore.ui.effect.FavouritesUiEffect
import com.guidovezzoni.fakestore.ui.intent.FavouritesUiIntent
import com.guidovezzoni.fakestore.ui.state.FavouritesUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class FavouritesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Task 12.9 — factory helper with defaults for all constructor dependencies
    private fun createViewModel(
        getProductsUseCase: GetProductsUseCase = mockk(),
        getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk<GetFavouriteIdsUseCase>().also { mock ->
            every { mock() } returns flowOf(emptySet())
        },
        toggleFavouriteUseCase: ToggleFavouriteUseCase = mockk<ToggleFavouriteUseCase>().also { mock ->
            coEvery { mock(any(), any()) } returns Result.success(Unit)
        },
        analyticsClient: AnalyticsClient = mockk(relaxed = true),
    ) = FavouritesViewModel(getProductsUseCase, getFavouriteIdsUseCase, toggleFavouriteUseCase, analyticsClient)

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

    // Task 12.1
    @Test
    fun `GIVEN a newly constructed FavouritesViewModel WHEN no intent has been dispatched THEN uiState value is FavouritesUiState Loading`() {
        val expected = FavouritesUiState.Loading

        val viewModel = createViewModel()
        val result = viewModel.uiState.value

        assertEquals(expected, result)
    }

    // Task 12.2
    @Test
    fun `GIVEN products with ids 1 2 3 and GetFavouriteIdsUseCase emitting setOf 2 WHEN LoadFavourites is dispatched THEN uiState is Content with exactly one item with id 2 and isFavourite true`() = runTest {
        val products = listOf(
            createProduct(id = FIRST_PRODUCT_ID),
            createProduct(id = SECOND_PRODUCT_ID),
            createProduct(id = THIRD_PRODUCT_ID),
        )
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(products))
        val getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk()
        every { getFavouriteIdsUseCase() } returns flowOf(setOf(SECOND_PRODUCT_ID))
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            getFavouriteIdsUseCase = getFavouriteIdsUseCase,
        )

        viewModel.onIntent(FavouritesUiIntent.LoadFavourites)
        val content = viewModel.uiState.value as FavouritesUiState.Content

        val expectedSize = 1
        assertEquals(expectedSize, content.products.size)
        assertEquals(SECOND_PRODUCT_ID, content.products[0].id)
        val expectedIsFavourite = true
        assertEquals(expectedIsFavourite, content.products[0].isFavourite)
    }

    // Task 12.3
    @Test
    fun `GIVEN a non-empty product list and GetFavouriteIdsUseCase emitting emptySet WHEN LoadFavourites is dispatched THEN uiState equals Content with emptyList`() = runTest {
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(listOf(createProduct())))
        val getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk()
        every { getFavouriteIdsUseCase() } returns flowOf(emptySet())
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            getFavouriteIdsUseCase = getFavouriteIdsUseCase,
        )

        viewModel.onIntent(FavouritesUiIntent.LoadFavourites)
        val result = viewModel.uiState.value

        val expected = FavouritesUiState.Content(products = emptyList())
        assertEquals(expected, result)
    }

    // Task 12.4
    @Test
    fun `GIVEN LoadFavourites dispatched with product id 2 in Content WHEN GetFavouriteIdsUseCase emits a set no longer containing 2 THEN uiState no longer contains that product`() = runTest {
        val products = listOf(createProduct(id = SECOND_PRODUCT_ID))
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(products))
        val favouriteIdsFlow = MutableStateFlow<Set<Int>>(setOf(SECOND_PRODUCT_ID))
        val getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk()
        every { getFavouriteIdsUseCase() } returns favouriteIdsFlow
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            getFavouriteIdsUseCase = getFavouriteIdsUseCase,
        )

        viewModel.onIntent(FavouritesUiIntent.LoadFavourites)
        favouriteIdsFlow.value = emptySet()
        val content = viewModel.uiState.value as FavouritesUiState.Content

        val expectedSize = 0
        assertEquals(expectedSize, content.products.size)
    }

    // Task 12.5
    @Test
    fun `GIVEN Content with product id 7 WHEN ToggleFavourite is dispatched before write completes THEN uiState products no longer contains product with id 7`() = runTest {
        val product = createProduct(id = TOGGLE_PRODUCT_ID)
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(listOf(product)))
        val getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk()
        every { getFavouriteIdsUseCase() } returns flowOf(setOf(TOGGLE_PRODUCT_ID))
        val deferred = CompletableDeferred<Result<Unit>>()
        val toggleFavouriteUseCase: ToggleFavouriteUseCase = mockk()
        coEvery { toggleFavouriteUseCase(TOGGLE_PRODUCT_ID, false) } coAnswers { deferred.await() }
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            getFavouriteIdsUseCase = getFavouriteIdsUseCase,
            toggleFavouriteUseCase = toggleFavouriteUseCase,
        )

        viewModel.onIntent(FavouritesUiIntent.LoadFavourites)
        viewModel.onIntent(FavouritesUiIntent.ToggleFavourite(productId = TOGGLE_PRODUCT_ID))
        val content = viewModel.uiState.value as FavouritesUiState.Content

        val expectedContainsProduct = false
        assertEquals(expectedContainsProduct, content.products.any { it.id == TOGGLE_PRODUCT_ID })

        deferred.cancel()
    }

    // Task 12.6
    @Test
    fun givenContentWithProductId7AndFailedToggle_whenToggleFavouriteDispatched_thenProductRestoredAndEffectEmitted() = runTest {
        val product = createProduct(id = TOGGLE_PRODUCT_ID)
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(listOf(product)))
        val getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk()
        every { getFavouriteIdsUseCase() } returns flowOf(setOf(TOGGLE_PRODUCT_ID))
        val toggleFavouriteUseCase: ToggleFavouriteUseCase = mockk()
        coEvery { toggleFavouriteUseCase(TOGGLE_PRODUCT_ID, false) } returns Result.failure(RuntimeException("db error"))
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            getFavouriteIdsUseCase = getFavouriteIdsUseCase,
            toggleFavouriteUseCase = toggleFavouriteUseCase,
        )

        val effectDeferred = async(testDispatcher) { viewModel.uiEffect.first() }

        viewModel.onIntent(FavouritesUiIntent.LoadFavourites)
        viewModel.onIntent(FavouritesUiIntent.ToggleFavourite(productId = TOGGLE_PRODUCT_ID))

        val content = viewModel.uiState.value as FavouritesUiState.Content
        val expectedContainsProduct = true
        assertEquals(expectedContainsProduct, content.products.any { it.id == TOGGLE_PRODUCT_ID })

        val expectedEffect = FavouritesUiEffect.ShowFavouriteToggleError
        assertEquals(expectedEffect, effectDeferred.await())
    }

    // Task 12.7
    @Test
    fun givenSuccessfulToggle_whenToggleFavouriteDispatched_thenFavouriteRemovedLoggedOnce() = runTest {
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

        viewModel.onIntent(FavouritesUiIntent.LoadFavourites)
        viewModel.onIntent(FavouritesUiIntent.ToggleFavourite(productId = TOGGLE_PRODUCT_ID))

        verify(exactly = 1) {
            analyticsClient.logEvent(name = "favourite_removed", params = mapOf("product_id" to TOGGLE_PRODUCT_ID))
        }
    }

    // Task 2.1
    @Test
    fun `GIVEN Content with 3 favourite products WHEN TrackScreenViewed is dispatched THEN favourites_screen_viewed is logged with favourite_count = 3`() = runTest {
        val products = listOf(
            createProduct(id = FIRST_PRODUCT_ID),
            createProduct(id = SECOND_PRODUCT_ID),
            createProduct(id = THIRD_PRODUCT_ID),
        )
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(products))
        val getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk()
        every { getFavouriteIdsUseCase() } returns flowOf(setOf(FIRST_PRODUCT_ID, SECOND_PRODUCT_ID, THIRD_PRODUCT_ID))
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            getFavouriteIdsUseCase = getFavouriteIdsUseCase,
            analyticsClient = analyticsClient,
        )

        viewModel.onIntent(FavouritesUiIntent.LoadFavourites)
        viewModel.onIntent(FavouritesUiIntent.TrackScreenViewed)

        val expectedCount = 3
        verify(exactly = 1) {
            analyticsClient.logEvent(name = EVENT_FAVOURITES_SCREEN_VIEWED, params = mapOf(PARAM_FAVOURITE_COUNT to expectedCount))
        }
    }

    // Task 2.2
    @Test
    fun `GIVEN Content with empty product list WHEN TrackScreenViewed is dispatched THEN favourites_screen_viewed is logged with favourite_count = 0`() = runTest {
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(listOf(createProduct())))
        val getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk()
        every { getFavouriteIdsUseCase() } returns flowOf(emptySet())
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            getFavouriteIdsUseCase = getFavouriteIdsUseCase,
            analyticsClient = analyticsClient,
        )

        viewModel.onIntent(FavouritesUiIntent.LoadFavourites)
        viewModel.onIntent(FavouritesUiIntent.TrackScreenViewed)

        val expectedCount = 0
        verify(exactly = 1) {
            analyticsClient.logEvent(name = EVENT_FAVOURITES_SCREEN_VIEWED, params = mapOf(PARAM_FAVOURITE_COUNT to expectedCount))
        }
    }

    // Task 2.3
    @Test
    fun `GIVEN Loading state WHEN TrackScreenViewed is dispatched THEN no analytics event is logged`() = runTest {
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(analyticsClient = analyticsClient)

        viewModel.onIntent(FavouritesUiIntent.TrackScreenViewed)

        verify(exactly = 0) { analyticsClient.logEvent(any(), any()) }
    }

    // Task 2.4
    @Test
    fun `GIVEN Error state WHEN TrackScreenViewed is dispatched THEN no analytics event is logged`() = runTest {
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.failure(RuntimeException("network error")))
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            analyticsClient = analyticsClient,
        )

        viewModel.onIntent(FavouritesUiIntent.LoadFavourites)
        viewModel.onIntent(FavouritesUiIntent.TrackScreenViewed)

        verify(exactly = 0) { analyticsClient.logEvent(any(), any()) }
    }

    // Task 2.5
    @Test
    fun `GIVEN Content WHEN TrackScreenViewed is dispatched twice THEN favourites_screen_viewed is logged twice`() = runTest {
        val products = listOf(createProduct(id = FIRST_PRODUCT_ID))
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(products))
        val getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk()
        every { getFavouriteIdsUseCase() } returns flowOf(setOf(FIRST_PRODUCT_ID))
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            getFavouriteIdsUseCase = getFavouriteIdsUseCase,
            analyticsClient = analyticsClient,
        )

        viewModel.onIntent(FavouritesUiIntent.LoadFavourites)
        viewModel.onIntent(FavouritesUiIntent.TrackScreenViewed)
        viewModel.onIntent(FavouritesUiIntent.TrackScreenViewed)

        verify(exactly = 2) {
            analyticsClient.logEvent(name = EVENT_FAVOURITES_SCREEN_VIEWED, params = mapOf(PARAM_FAVOURITE_COUNT to 1))
        }
    }

    @Test
    fun `GIVEN Content after first LoadFavourites WHEN LoadFavourites is dispatched again THEN uiState remains Content and does not reset to Loading`() = runTest {
        val products = listOf(createProduct(id = FIRST_PRODUCT_ID))
        val getProductsUseCase: GetProductsUseCase = mockk()
        every { getProductsUseCase() } returns flowOf(Result.success(products))
        val getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk()
        every { getFavouriteIdsUseCase() } returns flowOf(setOf(FIRST_PRODUCT_ID))
        val viewModel = createViewModel(
            getProductsUseCase = getProductsUseCase,
            getFavouriteIdsUseCase = getFavouriteIdsUseCase,
        )

        viewModel.onIntent(FavouritesUiIntent.LoadFavourites)
        val stateAfterFirstLoad = viewModel.uiState.value
        val expectedIsContent = true
        assertEquals(expectedIsContent, stateAfterFirstLoad is FavouritesUiState.Content)

        viewModel.onIntent(FavouritesUiIntent.LoadFavourites)
        val stateAfterSecondLoad = viewModel.uiState.value

        val expectedIsStillContent = true
        assertEquals(expectedIsStillContent, stateAfterSecondLoad is FavouritesUiState.Content)
    }

    private companion object {
        const val EVENT_FAVOURITES_SCREEN_VIEWED = "favourites_screen_viewed"
        const val PARAM_FAVOURITE_COUNT = "favourite_count"
        const val PRODUCT_ID = 1
        const val PRODUCT_TITLE = "Test Product"
        const val PRODUCT_PRICE = 109.95
        const val PRODUCT_DESCRIPTION = "A test product"
        const val PRODUCT_CATEGORY = "test"
        const val PRODUCT_IMAGE_URL = "https://example.com/image.png"
        const val PRODUCT_RATING_SCORE = 4.1
        const val PRODUCT_RATING_COUNT = 100
        const val FIRST_PRODUCT_ID = 1
        const val SECOND_PRODUCT_ID = 2
        const val THIRD_PRODUCT_ID = 3
        const val TOGGLE_PRODUCT_ID = 7
    }
}
