package com.guidovezzoni.fakestore.data.mapper

import com.guidovezzoni.fakestore.data.model.ProductDto
import com.guidovezzoni.fakestore.data.model.RatingDto
import com.guidovezzoni.fakestore.domain.model.Product
import com.guidovezzoni.fakestore.domain.model.Rating
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductMapperTest {
    @Test
    fun `GIVEN ProductDto with nested RatingDto WHEN mapped THEN all fields map correctly including Rating`() {
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

        val expectedProduct = Product(
            id = 1,
            title = "Backpack",
            price = 109.95,
            description = "desc",
            category = "men's clothing",
            imageUrl = "https://example.com/img.png",
            rating = Rating(score = 3.9, count = 120),
        )

        val actualProduct = ProductMapper.map(productDto)

        assertEquals(expectedProduct, actualProduct)
    }
}
