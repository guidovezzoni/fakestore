package com.guidovezzoni.fakestore.domain.usecase

import com.guidovezzoni.fakestore.domain.repository.FavouritesRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.Runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleFavouriteUseCaseTest {

    @MockK
    private lateinit var repository: FavouritesRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    private fun createUseCase(): ToggleFavouriteUseCase = ToggleFavouriteUseCase(repository)

    @Test
    fun `GIVEN ToggleFavouriteUseCase backed by mocked FavouritesRepository WHEN invoke(productId = 7, shouldBeFavourite = true) is called THEN addFavourite(7) is invoked exactly once and the result is success`() =
        runTest {
            coEvery { repository.addFavourite(7) } just Runs
            val useCase = createUseCase()

            val result = useCase(productId = 7, shouldBeFavourite = true)

            coVerify(exactly = 1) { repository.addFavourite(7) }
            assertTrue(result.isSuccess)
        }

    @Test
    fun `GIVEN ToggleFavouriteUseCase backed by mocked FavouritesRepository WHEN invoke(productId = 7, shouldBeFavourite = false) is called THEN removeFavourite(7) is invoked exactly once and the result is success`() =
        runTest {
            coEvery { repository.removeFavourite(7) } just Runs
            val useCase = createUseCase()

            val result = useCase(productId = 7, shouldBeFavourite = false)

            coVerify(exactly = 1) { repository.removeFavourite(7) }
            assertTrue(result.isSuccess)
        }

    @Test
    fun `GIVEN mocked FavouritesRepository addFavourite throws WHEN invoke(productId = 7, shouldBeFavourite = true) is called THEN result is failure wrapping the exception`() =
        runTest {
            val exception = RuntimeException("db error")
            coEvery { repository.addFavourite(7) } throws exception
            val useCase = createUseCase()

            val result = useCase(productId = 7, shouldBeFavourite = true)

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }
}
