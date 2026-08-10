package com.guidovezzoni.fakestore.domain.usecase

import com.guidovezzoni.fakestore.domain.model.Product
import com.guidovezzoni.fakestore.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class GetProductsUseCase(private val repository: ProductRepository) {
    operator fun invoke(): Flow<Result<List<Product>>> =
        flow { emit(Result.success(repository.getProducts())) }
            .catch { emit(Result.failure(it)) }
}
