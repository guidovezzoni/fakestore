package com.guidovezzoni.fakestore.domain.usecase

import com.guidovezzoni.fakestore.domain.model.Product
import com.guidovezzoni.fakestore.domain.model.Rating
import com.guidovezzoni.fakestore.domain.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetProductsUseCaseTest {

    private val repository: ProductRepository = mockk()
    private val useCase = GetProductsUseCase(repository)

    @Test
    fun `GIVEN repository returns products WHEN use case invoked THEN emits Result success`() = runTest {
        val products = listOf(
            Product(
                id = 1,
                title = "Backpack",
                price = 109.95,
                description = "desc",
                category = "men's clothing",
                imageUrl = "https://example.com/img.png",
                rating = Rating(score = 3.9, count = 120),
            )
        )
        coEvery { repository.getProducts() } returns products

        val emissions = useCase().toList()

        assertEquals(1, emissions.size)
        val expectedResult = Result.success(products)
        assertEquals(expectedResult, emissions[0])
    }

    @Test
    fun `GIVEN repository throws exception WHEN use case invoked THEN emits Result failure`() = runTest {
        val expectedException = RuntimeException("Network error")
        coEvery { repository.getProducts() } throws expectedException

        val emissions = useCase().toList()

        assertEquals(1, emissions.size)
        val expectedResult = Result.failure<List<Product>>(expectedException)
        assertEquals(expectedResult, emissions[0])
        assertTrue(emissions[0].isFailure)
        assertEquals(expectedException, emissions[0].exceptionOrNull())
    }
}
