package com.guidovezzoni.fakestore.domain.usecase

import com.guidovezzoni.fakestore.domain.repository.FavouritesRepository
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetFavouriteIdsUseCaseTest {

    @MockK
    lateinit var repository: FavouritesRepository

    private lateinit var useCase: GetFavouriteIdsUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = GetFavouriteIdsUseCase(repository)
    }

    @Test
    fun `GIVEN repository emits setOf(1, 2) WHEN invoke() is collected THEN it emits setOf(1, 2)`() = runTest {
        every { repository.getFavouriteIds() } returns flowOf(setOf(1, 2))

        val result = useCase().first()

        val expected = setOf(1, 2)
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN repository emits emptySet() WHEN invoke() is collected THEN it emits emptySet()`() = runTest {
        every { repository.getFavouriteIds() } returns flowOf(emptySet())

        val result = useCase().first()

        val expected = emptySet<Int>()
        assertEquals(expected, result)
    }
}
