package com.guidovezzoni.fakestore.ui.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private const val USD_CURRENCY_CODE = "USD"
private const val RATING_FRACTION_DIGITS = 1

fun formatPrice(price: Double, locale: Locale): String {
    val formatter = NumberFormat.getCurrencyInstance(locale)
    formatter.currency = Currency.getInstance(USD_CURRENCY_CODE)
    return formatter.format(price)
}

fun formatRatingScore(score: Double, locale: Locale): String {
    val formatter = NumberFormat.getNumberInstance(locale)
    formatter.minimumFractionDigits = RATING_FRACTION_DIGITS
    formatter.maximumFractionDigits = RATING_FRACTION_DIGITS
    return formatter.format(score)
}
