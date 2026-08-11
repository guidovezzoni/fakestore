package com.guidovezzoni.fakestore.ui.util

import com.guidovezzoni.fakestore.domain.model.Product
import com.guidovezzoni.fakestore.ui.state.ProductListItem
import java.util.Locale

internal fun mapToProductListItem(product: Product, locale: Locale): ProductListItem =
    ProductListItem(
        id = product.id,
        imageUrl = product.imageUrl,
        title = product.title,
        formattedPrice = formatPrice(product.price, locale),
        formattedRatingScore = formatRatingScore(product.rating.score, locale),
    )
