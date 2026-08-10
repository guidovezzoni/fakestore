package com.guidovezzoni.fakestore.data.network

import com.guidovezzoni.fakestore.data.model.ProductDto
import retrofit2.http.GET

interface ApiService {
    @GET("products")
    suspend fun getProducts(): List<ProductDto>
}
