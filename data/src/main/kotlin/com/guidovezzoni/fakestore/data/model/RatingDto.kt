package com.guidovezzoni.fakestore.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RatingDto(
    val rate: Double,
    val count: Int,
)
