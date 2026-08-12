package com.guidovezzoni.fakestore.ui.util

import com.guidovezzoni.fakestore.domain.model.Product
import com.guidovezzoni.fakestore.domain.model.Rating
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductListItemMapperTest {

    private fun createProduct(
        id: Int = PRODUCT_ID,
        title: String = PRODUCT_TITLE,
        price: Double = PRODUCT_PRICE,
        description: String = PRODUCT_DESCRIPTION,
        category: String = PRODUCT_CATEGORY,
        imageUrl: String = PRODUCT_IMAGE_URL,
        ratingScore: Double = PRODUCT_RATING_SCORE,
        ratingCount: Int = PRODUCT_RATING_COUNT,
    ) = Product(
        id = id,
        title = title,
        price = price,
        description = description,
        category = category,
        imageUrl = imageUrl,
        rating = Rating(score = ratingScore, count = ratingCount),
    )

    @Test
    fun `GIVEN a product WHEN mapToProductListItem is called THEN id is mapped correctly`() {
        val product = createProduct(id = PRODUCT_ID)

        val result = mapToProductListItem(product, Locale.US, isFavourite = false)

        assertEquals(PRODUCT_ID, result.id)
    }

    @Test
    fun `GIVEN a product WHEN mapToProductListItem is called THEN imageUrl is mapped correctly`() {
        val product = createProduct(imageUrl = PRODUCT_IMAGE_URL)

        val result = mapToProductListItem(product, Locale.US, isFavourite = false)

        assertEquals(PRODUCT_IMAGE_URL, result.imageUrl)
    }

    @Test
    fun `GIVEN a product WHEN mapToProductListItem is called THEN title is mapped correctly`() {
        val product = createProduct(title = PRODUCT_TITLE)

        val result = mapToProductListItem(product, Locale.US, isFavourite = false)

        assertEquals(PRODUCT_TITLE, result.title)
    }

    @Test
    fun `GIVEN a product with price 109_95 WHEN mapToProductListItem is called with Locale_US THEN formattedPrice is dollar sign 109_95`() {
        val product = createProduct(price = PRODUCT_PRICE)
        val expected = "$109.95"

        val result = mapToProductListItem(product, Locale.US, isFavourite = false)

        assertEquals(expected, result.formattedPrice)
    }

    @Test
    fun `GIVEN a product with rating score 4_1 WHEN mapToProductListItem is called with Locale_US THEN formattedRatingScore is 4_1`() {
        val product = createProduct(ratingScore = PRODUCT_RATING_SCORE)
        val expected = "4.1"

        val result = mapToProductListItem(product, Locale.US, isFavourite = false)

        assertEquals(expected, result.formattedRatingScore)
    }

    @Test
    fun `GIVEN a product with rating score 4_1 WHEN mapToProductListItem is called with Locale es_ES THEN formattedRatingScore uses comma as decimal separator`() {
        val product = createProduct(ratingScore = PRODUCT_RATING_SCORE)
        val expected = "4,1"

        val result = mapToProductListItem(product, Locale("es", "ES"), isFavourite = false)

        assertEquals(expected, result.formattedRatingScore)
    }

    @Test
    fun `GIVEN a product and isFavourite true WHEN mapToProductListItem is called with isFavourite true THEN isFavourite is true`() {
        val product = createProduct()
        val expected = true

        val result = mapToProductListItem(product, Locale.US, isFavourite = true)

        assertEquals(expected, result.isFavourite)
    }

    @Test
    fun `GIVEN a product and isFavourite false WHEN mapToProductListItem is called with isFavourite false THEN isFavourite is false`() {
        val product = createProduct()
        val expected = false

        val result = mapToProductListItem(product, Locale.US, isFavourite = false)

        assertEquals(expected, result.isFavourite)
    }

    private companion object {
        const val PRODUCT_ID = 1
        const val PRODUCT_TITLE = "Test Product"
        const val PRODUCT_PRICE = 109.95
        const val PRODUCT_DESCRIPTION = "A test product"
        const val PRODUCT_CATEGORY = "test"
        const val PRODUCT_IMAGE_URL = "https://example.com/image.png"
        const val PRODUCT_RATING_SCORE = 4.1
        const val PRODUCT_RATING_COUNT = 100
    }
}
