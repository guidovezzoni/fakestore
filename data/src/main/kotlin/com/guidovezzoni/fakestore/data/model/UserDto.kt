package com.guidovezzoni.fakestore.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int,
    val email: String,
    val username: String,
    val name: UserNameDto,
)
