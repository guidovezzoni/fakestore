package com.guidovezzoni.fakestore.data.mapper

import com.guidovezzoni.fakestore.data.model.UserDto
import com.guidovezzoni.fakestore.data.model.UserNameDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UserMapperTest {

    @Test
    fun `GIVEN a UserDto with lowercase name fields WHEN mapped THEN the domain UserName fields preserve the original casing exactly`() {
        val userNameDto = UserNameDto(firstname = "john", lastname = "doe")
        val userDto = UserDto(
            id = 1,
            email = "john.doe@example.com",
            username = "johnd",
            name = userNameDto,
        )

        val result = UserMapper.map(userDto)

        assertEquals("john", result.name.firstName)
        assertEquals("doe", result.name.lastName)
    }

    @Test
    fun `GIVEN a UserDto with all fields populated WHEN mapped THEN all domain model fields are correctly set`() {
        val userNameDto = UserNameDto(firstname = "Jane", lastname = "Smith")
        val userDto = UserDto(
            id = 42,
            email = "jane.smith@example.com",
            username = "janesmith",
            name = userNameDto,
        )

        val result = UserMapper.map(userDto)

        assertEquals(42, result.id)
        assertEquals("janesmith", result.userName)
        assertEquals("Jane", result.name.firstName)
        assertEquals("Smith", result.name.lastName)
        assertEquals("jane.smith@example.com", result.email)
    }

    @Test
    fun `GIVEN UserDto's declared fields WHEN inspected via reflection THEN no field named password, phone, address, or __v exists`() {
        val forbiddenFieldNames = setOf("password", "phone", "address", "__v")
        val declaredFieldNames = UserDto::class.java.declaredFields.map { it.name }.toSet()

        val unexpectedFields = declaredFieldNames.intersect(forbiddenFieldNames)

        assertFalse(
            "UserDto must not declare sensitive fields: $unexpectedFields",
            unexpectedFields.isNotEmpty(),
        )
    }
}
