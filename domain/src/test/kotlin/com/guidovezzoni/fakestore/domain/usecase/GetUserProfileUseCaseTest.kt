package com.guidovezzoni.fakestore.domain.usecase

import com.guidovezzoni.fakestore.domain.model.UserName
import com.guidovezzoni.fakestore.domain.model.UserProfile
import com.guidovezzoni.fakestore.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetUserProfileUseCaseTest {

    private val repository: UserRepository = mockk()
    private val useCase = GetUserProfileUseCase(repository)

    @Test
    fun `GIVEN repository returns a valid UserProfile WHEN use case is invoked THEN it emits Result success with the profile and the Flow completes`() = runTest {
        val userProfile = UserProfile(
            id = 8,
            userName = "johnd",
            name = UserName(firstName = "John", lastName = "Doe"),
            email = "john@example.com",
        )
        coEvery { repository.getUserProfile(8) } returns userProfile

        val emissions = useCase(8).toList()

        assertEquals(1, emissions.size)
        val expectedResult = Result.success(userProfile)
        assertEquals(expectedResult, emissions[0])
    }

    @Test
    fun `GIVEN repository throws an exception WHEN use case is invoked THEN it emits Result failure with the exception and the Flow completes`() = runTest {
        val expectedException = RuntimeException("Network error")
        coEvery { repository.getUserProfile(8) } throws expectedException

        val emissions = useCase(8).toList()

        assertEquals(1, emissions.size)
        val expectedResult = Result.failure<UserProfile>(expectedException)
        assertEquals(expectedResult, emissions[0])
        assertTrue(emissions[0].isFailure)
        assertEquals(expectedException, emissions[0].exceptionOrNull())
    }
}
