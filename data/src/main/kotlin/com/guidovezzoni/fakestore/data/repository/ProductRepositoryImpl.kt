package com.guidovezzoni.fakestore.data.repository

import com.guidovezzoni.fakestore.data.mapper.ProductMapper
import com.guidovezzoni.fakestore.data.network.ApiService
import com.guidovezzoni.fakestore.domain.model.Product
import com.guidovezzoni.fakestore.domain.repository.ProductRepository

class ProductRepositoryImpl(private val apiService: ApiService) : ProductRepository {
    override suspend fun getProducts(): List<Product> =
        apiService.getProducts().map { ProductMapper.map(it) }
}
