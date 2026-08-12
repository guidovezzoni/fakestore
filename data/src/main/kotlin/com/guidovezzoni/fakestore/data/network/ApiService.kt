package com.guidovezzoni.fakestore.data.network

import com.guidovezzoni.fakestore.data.model.ProductDto
import com.guidovezzoni.fakestore.data.model.UserDto
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("products")
    suspend fun getProducts(): List<ProductDto>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Int): UserDto
}

