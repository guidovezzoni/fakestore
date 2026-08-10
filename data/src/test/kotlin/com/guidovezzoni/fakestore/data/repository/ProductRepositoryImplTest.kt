package com.guidovezzoni.fakestore.data.repository

import com.guidovezzoni.fakestore.data.mapper.ProductMapper
import com.guidovezzoni.fakestore.data.model.ProductDto
import com.guidovezzoni.fakestore.data.model.RatingDto
import com.guidovezzoni.fakestore.data.network.ApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ProductRepositoryImplTest {

    private val apiService: ApiService = mockk()
    private val repository = ProductRepositoryImpl(apiService)

    @Test
    fun `GIVEN ApiService returns ProductDto list WHEN called THEN returns mapped domain models`() = runTest {
        val ratingDto = RatingDto(rate = 3.9, count = 120)
        val productDto = ProductDto(
            id = 1,
            title = "Backpack",
            price = 109.95,
            description = "desc",
            category = "men's clothing",
            image = "https://example.com/img.png",
            rating = ratingDto,
        )
        coEvery { apiService.getProducts() } returns listOf(productDto)

        val expectedProducts = listOf(ProductMapper.map(productDto))
        val actualProducts = repository.getProducts()

        assertEquals(expectedProducts, actualProducts)
    }

    @Test
    fun `GIVEN ApiService throws IOException WHEN getProducts called THEN exception propagates unchanged`() = runTest {
        val expectedException = IOException("Network error")
        coEvery { apiService.getProducts() } throws expectedException

        val thrownException = runCatching { repository.getProducts() }.exceptionOrNull()

        assertEquals(expectedException, thrownException)
    }
}
