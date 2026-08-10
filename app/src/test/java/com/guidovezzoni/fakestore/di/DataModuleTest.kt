package com.guidovezzoni.fakestore.di

import com.guidovezzoni.fakestore.data.network.ApiService
import com.guidovezzoni.fakestore.data.repository.ProductRepositoryImpl
import com.guidovezzoni.fakestore.domain.model.Product
import com.guidovezzoni.fakestore.domain.model.Rating
import com.guidovezzoni.fakestore.domain.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataModuleTest {

    @Test
    fun `GIVEN DataModule provideProductRepository is called directly with a mock ApiService WHEN the return value type is inspected THEN it is ProductRepositoryImpl`() {
        val mockApiService: ApiService = mockk()

        val repository = DataModule.provideProductRepository(mockApiService)

        assertTrue(repository is ProductRepositoryImpl)
    }

    @Test
    fun `GIVEN DataModule provideGetProductsUseCase is called with a mock ProductRepository WHEN the use case is invoked and collected THEN it emits Result success wrapping the known product list`() = runTest {
        val knownProducts = listOf(
            Product(
                id = 1,
                title = "Test Product",
                price = 29.99,
                description = "A test product",
                category = "test",
                imageUrl = "https://example.com/image.png",
                rating = Rating(score = 4.5, count = 100),
            )
        )
        val mockRepository: ProductRepository = mockk()
        coEvery { mockRepository.getProducts() } returns knownProducts

        val useCase = DataModule.provideGetProductsUseCase(mockRepository)
        val emissions = useCase().toList()

        assertEquals(1, emissions.size)
        val expectedResult = Result.success(knownProducts)
        assertEquals(expectedResult, emissions[0])
    }

    @Test
    fun `GIVEN DataModule provideApiService is called with a real Retrofit instance WHEN the return value type is inspected THEN it is a non-null ApiService proxy`() {
        val networkClient = NetworkModule.provideNetworkClient()
        val retrofit = NetworkModule.provideRetrofit(networkClient)

        val apiService = DataModule.provideApiService(retrofit)

        assertNotNull(apiService)
    }
}
