package com.guidovezzoni.fakestore.domain.model

data class UserProfile(
    val id: Int,
    val userName: String,
    val name: UserName,
    val email: String,
)
