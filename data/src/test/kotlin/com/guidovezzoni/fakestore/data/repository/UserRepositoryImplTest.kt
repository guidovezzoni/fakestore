package com.guidovezzoni.fakestore.data.repository

import com.guidovezzoni.fakestore.data.model.UserDto
import com.guidovezzoni.fakestore.data.model.UserNameDto
import com.guidovezzoni.fakestore.data.network.ApiService
import com.guidovezzoni.fakestore.domain.model.UserName
import com.guidovezzoni.fakestore.domain.model.UserProfile
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class UserRepositoryImplTest {

    private val apiService: ApiService = mockk()
    private val repository = UserRepositoryImpl(apiService)

    @Test
    fun `GIVEN the API returns a valid UserDto WHEN getUserProfile is called THEN it returns a correctly mapped UserProfile domain model`() = runTest {
        val userNameDto = UserNameDto(firstname = "John", lastname = "Doe")
        val userDto = UserDto(
            id = 8,
            email = "john.doe@example.com",
            username = "johnd",
            name = userNameDto,
        )
        coEvery { apiService.getUser(8) } returns userDto

        val expectedProfile = UserProfile(
            id = 8,
            userName = "johnd",
            name = UserName(firstName = "John", lastName = "Doe"),
            email = "john.doe@example.com",
        )
        val actualProfile = repository.getUserProfile(8)

        assertEquals(expectedProfile, actualProfile)
    }

    @Test
    fun `GIVEN ApiService getUser throws an IOException WHEN getUserProfile is called THEN the exception propagates unchanged`() = runTest {
        val expectedException = IOException("Network error")
        coEvery { apiService.getUser(8) } throws expectedException

        val thrownException = runCatching { repository.getUserProfile(8) }.exceptionOrNull()

        assertEquals(expectedException, thrownException)
    }
}
