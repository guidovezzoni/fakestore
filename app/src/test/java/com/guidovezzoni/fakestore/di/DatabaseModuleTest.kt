package com.guidovezzoni.fakestore.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.guidovezzoni.fakestore.data.database.FavouriteDao
import com.guidovezzoni.fakestore.data.database.FavouritesDatabase
import com.guidovezzoni.fakestore.data.repository.FavouritesRepositoryImpl
import com.guidovezzoni.fakestore.domain.repository.FavouritesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.Runs
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseModuleTest {

    @Test
    fun `GIVEN DatabaseModule provideFavouritesDatabase is called with a mocked Context WHEN the returned FavouritesDatabase's favouriteDao is inspected THEN it is non-null`() {
        val mockContext = mockk<Context>(relaxed = true)
        val mockDao = mockk<FavouriteDao>()
        val mockDatabase = mockk<FavouritesDatabase>()
        val mockBuilder = mockk<RoomDatabase.Builder<FavouritesDatabase>>()

        mockkStatic(Room::class)
        every { Room.databaseBuilder(mockContext, FavouritesDatabase::class.java, any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockDatabase
        every { mockDatabase.favouriteDao() } returns mockDao

        val database = DatabaseModule.provideFavouritesDatabase(mockContext)
        val dao = database.favouriteDao()

        assertNotNull(dao)

        unmockkStatic(Room::class)
    }

    @Test
    fun `GIVEN DatabaseModule provideFavouritesRepository is called with a mocked FavouriteDao WHEN the return value type is inspected THEN it is FavouritesRepositoryImpl`() {
        val mockDao = mockk<FavouriteDao>()

        val repository = DatabaseModule.provideFavouritesRepository(mockDao)

        assertTrue(repository is FavouritesRepositoryImpl)
    }

    @Test
    fun `GIVEN DatabaseModule provideToggleFavouriteUseCase is called with a mocked FavouritesRepository WHEN the returned ToggleFavouriteUseCase is invoked THEN it delegates to the given repository`() = runTest {
        val repository = mockk<FavouritesRepository>()
        coEvery { repository.addFavourite(7) } just Runs

        val useCase = DatabaseModule.provideToggleFavouriteUseCase(repository)
        useCase(7, shouldBeFavourite = true)

        coVerify { repository.addFavourite(7) }
    }

    @Test
    fun `GIVEN DatabaseModule provideGetFavouriteIdsUseCase is called with a mocked FavouritesRepository WHEN the returned GetFavouriteIdsUseCase is invoked and collected THEN it delegates to the given repository's getFavouriteIds`() = runTest {
        val repository = mockk<FavouritesRepository>()
        every { repository.getFavouriteIds() } returns flowOf(setOf(1, 2))

        val useCase = DatabaseModule.provideGetFavouriteIdsUseCase(repository)
        val result = useCase().first()

        val expectedResult = setOf(1, 2)
        assertEquals(expectedResult, result)
    }
}
