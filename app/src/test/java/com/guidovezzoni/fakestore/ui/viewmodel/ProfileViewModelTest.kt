package com.guidovezzoni.fakestore.ui.viewmodel

import com.guidovezzoni.fakestore.core.analytics.AnalyticsClient
import com.guidovezzoni.fakestore.domain.model.UserName
import com.guidovezzoni.fakestore.domain.model.UserProfile
import com.guidovezzoni.fakestore.domain.usecase.GetFavouriteIdsUseCase
import com.guidovezzoni.fakestore.domain.usecase.GetUserProfileUseCase
import com.guidovezzoni.fakestore.ui.intent.ProfileUiIntent
import com.guidovezzoni.fakestore.ui.state.ProfileUiState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

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
        getUserProfileUseCase: GetUserProfileUseCase = mockk<GetUserProfileUseCase>().also { mock ->
            every { mock(any()) } returns flowOf(Result.success(createUserProfile()))
        },
        getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk<GetFavouriteIdsUseCase>().also { mock ->
            every { mock() } returns flowOf(emptySet())
        },
        analyticsClient: AnalyticsClient = mockk(relaxed = true),
    ) = ProfileViewModel(getUserProfileUseCase, getFavouriteIdsUseCase, analyticsClient)

    private fun createUserProfile(
        id: Int = USER_PROFILE_ID,
        userName: String = USER_PROFILE_USERNAME,
        firstName: String = USER_PROFILE_FIRST_NAME,
        lastName: String = USER_PROFILE_LAST_NAME,
        email: String = USER_PROFILE_EMAIL,
    ) = UserProfile(
        id = id,
        userName = userName,
        name = UserName(firstName = firstName, lastName = lastName),
        email = email,
    )

    // Task 6.1
    @Test
    fun `GIVEN a fresh ProfileViewModel WHEN LoadProfile is dispatched THEN state transitions to Content with correct fullName email and favouriteCount`() = runTest {
        val favouriteIds = setOf(FAVOURITE_ID_1, FAVOURITE_ID_2, FAVOURITE_ID_3)
        val getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk()
        every { getFavouriteIdsUseCase() } returns flowOf(favouriteIds)
        val viewModel = createViewModel(getFavouriteIdsUseCase = getFavouriteIdsUseCase)

        viewModel.onIntent(ProfileUiIntent.LoadProfile)
        val result = viewModel.uiState.value as ProfileUiState.Content

        val expectedFullName = "$USER_PROFILE_FIRST_NAME $USER_PROFILE_LAST_NAME"
        val expectedEmail = USER_PROFILE_EMAIL
        val expectedFavouriteCount = 3
        val expectedInitials = "${USER_PROFILE_FIRST_NAME.first()}${USER_PROFILE_LAST_NAME.first()}"
        assertEquals(expectedFullName, result.fullName)
        assertEquals(expectedEmail, result.email)
        assertEquals(expectedFavouriteCount, result.favouriteCount)
        assertEquals(expectedInitials, result.initials)
    }

    // Task 6.2
    @Test
    fun `GIVEN a fresh ProfileViewModel WHEN the profile API call fails THEN state transitions to Error`() = runTest {
        val getUserProfileUseCase: GetUserProfileUseCase = mockk()
        every { getUserProfileUseCase(any()) } returns flowOf(Result.failure(RuntimeException("network error")))
        val viewModel = createViewModel(getUserProfileUseCase = getUserProfileUseCase)

        viewModel.onIntent(ProfileUiIntent.LoadProfile)
        val result = viewModel.uiState.value

        val expected = ProfileUiState.Error
        assertEquals(expected, result)
    }

    // Task 6.3
    @Test
    fun `GIVEN Error state WHEN RetryClicked is dispatched THEN state transitions to Loading and re-invokes the use case`() = runTest {
        val getUserProfileUseCase: GetUserProfileUseCase = mockk()
        every { getUserProfileUseCase(any()) } returns flowOf(Result.failure(RuntimeException("network error")))
        val viewModel = createViewModel(getUserProfileUseCase = getUserProfileUseCase)

        viewModel.onIntent(ProfileUiIntent.LoadProfile)
        val stateAfterError = viewModel.uiState.value
        val expectedError = ProfileUiState.Error
        assertEquals(expectedError, stateAfterError)

        viewModel.onIntent(ProfileUiIntent.RetryClicked)

        verify(exactly = 2) { getUserProfileUseCase(any()) }
    }

    // Task 6.4
    @Test
    fun `GIVEN Content state WHEN GetFavouriteIdsUseCase emits a new set THEN favouriteCount updates reactively without re-fetching the profile`() = runTest {
        val favouriteIdsFlow = MutableStateFlow<Set<Int>>(setOf(FAVOURITE_ID_1))
        val getFavouriteIdsUseCase: GetFavouriteIdsUseCase = mockk()
        every { getFavouriteIdsUseCase() } returns favouriteIdsFlow
        val getUserProfileUseCase: GetUserProfileUseCase = mockk()
        every { getUserProfileUseCase(any()) } returns flowOf(Result.success(createUserProfile()))
        val viewModel = createViewModel(
            getUserProfileUseCase = getUserProfileUseCase,
            getFavouriteIdsUseCase = getFavouriteIdsUseCase,
        )

        viewModel.onIntent(ProfileUiIntent.LoadProfile)
        val initialContent = viewModel.uiState.value as ProfileUiState.Content
        val expectedInitialCount = 1
        assertEquals(expectedInitialCount, initialContent.favouriteCount)

        favouriteIdsFlow.value = setOf(FAVOURITE_ID_1, FAVOURITE_ID_2, FAVOURITE_ID_3)
        val updatedContent = viewModel.uiState.value as ProfileUiState.Content

        val expectedUpdatedCount = 3
        assertEquals(expectedUpdatedCount, updatedContent.favouriteCount)
        verify(exactly = 1) { getUserProfileUseCase(any()) }
    }

    // Task 6.5
    @Test
    fun `GIVEN Content state WHEN LoadProfile is dispatched again THEN GetUserProfileUseCase is not invoked a second time and state remains Content`() = runTest {
        val getUserProfileUseCase: GetUserProfileUseCase = mockk()
        every { getUserProfileUseCase(any()) } returns flowOf(Result.success(createUserProfile()))
        val viewModel = createViewModel(getUserProfileUseCase = getUserProfileUseCase)

        viewModel.onIntent(ProfileUiIntent.LoadProfile)
        val stateAfterFirstLoad = viewModel.uiState.value
        val expectedIsContent = true
        assertEquals(expectedIsContent, stateAfterFirstLoad is ProfileUiState.Content)

        viewModel.onIntent(ProfileUiIntent.LoadProfile)
        val stateAfterSecondLoad = viewModel.uiState.value

        val expectedIsStillContent = true
        assertEquals(expectedIsStillContent, stateAfterSecondLoad is ProfileUiState.Content)
        verify(exactly = 1) { getUserProfileUseCase(any()) }
    }

    // Task 6.6
    @Test
    fun `GIVEN a fresh ProfileViewModel WHEN TrackScreenViewed is dispatched and profile loads successfully THEN profile_screen_viewed is logged exactly once after reaching Content`() = runTest {
        val getUserProfileUseCase: GetUserProfileUseCase = mockk()
        every { getUserProfileUseCase(any()) } returns flowOf(Result.success(createUserProfile()))
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(
            getUserProfileUseCase = getUserProfileUseCase,
            analyticsClient = analyticsClient,
        )

        viewModel.onIntent(ProfileUiIntent.TrackScreenViewed)
        viewModel.onIntent(ProfileUiIntent.LoadProfile)

        verify(exactly = 1) { analyticsClient.logEvent(name = EVENT_PROFILE_SCREEN_VIEWED, params = any()) }
    }

    // Task 6.7
    @Test
    fun `GIVEN profile loading fails WHEN TrackScreenViewed was dispatched THEN the analytics event is NOT logged`() = runTest {
        val getUserProfileUseCase: GetUserProfileUseCase = mockk()
        every { getUserProfileUseCase(any()) } returns flowOf(Result.failure(RuntimeException("network error")))
        val analyticsClient: AnalyticsClient = mockk(relaxed = true)
        val viewModel = createViewModel(
            getUserProfileUseCase = getUserProfileUseCase,
            analyticsClient = analyticsClient,
        )

        viewModel.onIntent(ProfileUiIntent.TrackScreenViewed)
        viewModel.onIntent(ProfileUiIntent.LoadProfile)
        advanceUntilIdle()

        verify(exactly = 0) { analyticsClient.logEvent(any(), any()) }
    }

    // Task 6.8
    @Test
    fun `GIVEN TrackScreenViewed is dispatched WHEN AnalyticsClient logEvent is invoked THEN the call parameters contain no PII`() = runTest {
        val userProfile = createUserProfile(
            firstName = SENSITIVE_FIRST_NAME,
            lastName = SENSITIVE_LAST_NAME,
            email = SENSITIVE_EMAIL,
        )
        val getUserProfileUseCase: GetUserProfileUseCase = mockk()
        every { getUserProfileUseCase(any()) } returns flowOf(Result.success(userProfile))
        val analyticsClient: AnalyticsClient = mockk()
        val capturedParams = slot<Map<String, Any>>()
        every { analyticsClient.logEvent(any(), capture(capturedParams)) } just Runs
        val viewModel = createViewModel(
            getUserProfileUseCase = getUserProfileUseCase,
            analyticsClient = analyticsClient,
        )

        viewModel.onIntent(ProfileUiIntent.LoadProfile)
        viewModel.onIntent(ProfileUiIntent.TrackScreenViewed)

        val params = capturedParams.captured
        val paramsContainFirstName = params.values.any { it.toString().contains(SENSITIVE_FIRST_NAME) }
        val paramsContainLastName = params.values.any { it.toString().contains(SENSITIVE_LAST_NAME) }
        val paramsContainEmail = params.values.any { it.toString().contains(SENSITIVE_EMAIL) }
        assertFalse(paramsContainFirstName)
        assertFalse(paramsContainLastName)
        assertFalse(paramsContainEmail)
    }

    private companion object {
        const val EVENT_PROFILE_SCREEN_VIEWED = "profile_screen_viewed"
        const val USER_PROFILE_ID = 8
        const val USER_PROFILE_USERNAME = "testuser"
        const val USER_PROFILE_FIRST_NAME = "John"
        const val USER_PROFILE_LAST_NAME = "Doe"
        const val USER_PROFILE_EMAIL = "john.doe@example.com"
        const val FAVOURITE_ID_1 = 1
        const val FAVOURITE_ID_2 = 2
        const val FAVOURITE_ID_3 = 3
        const val SENSITIVE_FIRST_NAME = "Alice"
        const val SENSITIVE_LAST_NAME = "Smith"
        const val SENSITIVE_EMAIL = "alice.smith@example.com"
    }
}
