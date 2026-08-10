package com.guidovezzoni.fakestore.data.mapper

import com.guidovezzoni.fakestore.data.model.ProductDto
import com.guidovezzoni.fakestore.data.model.RatingDto
import com.guidovezzoni.fakestore.domain.model.Product
import com.guidovezzoni.fakestore.domain.model.Rating

internal object ProductMapper {
    fun map(dto: ProductDto): Product = Product(
        id = dto.id,
        title = dto.title,
        price = dto.price,
        description = dto.description,
        category = dto.category,
        imageUrl = dto.image,
        rating = map(dto.rating),
    )

    private fun map(dto: RatingDto): Rating = Rating(
        score = dto.rate,
        count = dto.count,
    )
}
