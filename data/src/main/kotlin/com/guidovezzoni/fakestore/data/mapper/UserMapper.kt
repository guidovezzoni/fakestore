package com.guidovezzoni.fakestore.data.mapper

import com.guidovezzoni.fakestore.data.model.UserDto
import com.guidovezzoni.fakestore.data.model.UserNameDto
import com.guidovezzoni.fakestore.domain.model.UserName
import com.guidovezzoni.fakestore.domain.model.UserProfile

internal object UserMapper {
    fun map(dto: UserDto): UserProfile = UserProfile(
        id = dto.id,
        userName = dto.username,
        name = map(dto.name),
        email = dto.email,
    )

    private fun map(dto: UserNameDto): UserName = UserName(
        firstName = dto.firstname,
        lastName = dto.lastname,
    )
}
