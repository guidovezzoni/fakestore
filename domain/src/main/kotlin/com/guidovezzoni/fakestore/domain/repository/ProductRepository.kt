package com.guidovezzoni.fakestore.domain.repository

import com.guidovezzoni.fakestore.domain.model.Product

interface ProductRepository {
    suspend fun getProducts(): List<Product>
}
