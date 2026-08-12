package com.guidovezzoni.fakestore.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserNameDto(
    val firstname: String,
    val lastname: String,
)
