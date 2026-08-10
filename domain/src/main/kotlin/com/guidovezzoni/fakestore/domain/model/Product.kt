package com.guidovezzoni.fakestore.domain.model

data class Product(
    val id: Int,
    val title: String,
    val price: Double,
    val description: String,
    val category: String,
    val imageUrl: String,
    val rating: Rating,
)
