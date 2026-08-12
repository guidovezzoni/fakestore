package com.guidovezzoni.fakestore.data.repository

import com.guidovezzoni.fakestore.data.database.FavouriteDao
import com.guidovezzoni.fakestore.data.database.FavouriteEntity
import io.mockk.MockKAnnotations
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FavouritesRepositoryImplTest {

    @MockK
    private lateinit var favouriteDao: FavouriteDao

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    private fun createRepository(favouriteDao: FavouriteDao = mockk()): FavouritesRepositoryImpl =
        FavouritesRepositoryImpl(favouriteDao)

    @Test
    fun `GIVEN FavouritesRepositoryImpl backed by mocked FavouriteDao WHEN addFavourite(productId = 7) is called THEN FavouriteDao insert is invoked exactly once`() =
        runTest {
            coJustRun { favouriteDao.insert(any()) }
            val repository = createRepository(favouriteDao)

            repository.addFavourite(productId = 7)

            coVerify(exactly = 1) { favouriteDao.insert(FavouriteEntity(productId = 7)) }
        }

    @Test
    fun `GIVEN FavouritesRepositoryImpl backed by mocked FavouriteDao WHEN removeFavourite(productId = 7) is called THEN FavouriteDao delete is invoked exactly once`() =
        runTest {
            coJustRun { favouriteDao.delete(any()) }
            val repository = createRepository(favouriteDao)

            repository.removeFavourite(productId = 7)

            coVerify(exactly = 1) { favouriteDao.delete(FavouriteEntity(productId = 7)) }
        }

    @Test
    fun `GIVEN FavouritesRepositoryImpl backed by mocked FavouriteDao whose getAllIds emits listOf(3, 7, 7) WHEN getFavouriteIds is collected THEN it emits setOf(3, 7)`() =
        runTest {
            every { favouriteDao.getAllIds() } returns flowOf(listOf(3, 7, 7))
            val repository = createRepository(favouriteDao)

            val result = repository.getFavouriteIds().first()

            val expectedResult = setOf(3, 7)
            assertEquals(expectedResult, result)
        }
}
