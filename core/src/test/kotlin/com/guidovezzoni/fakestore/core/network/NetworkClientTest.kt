package com.guidovezzoni.fakestore.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkClientTest {
    @Test
    fun `GIVEN the core network client factory WHEN the Retrofit instance is built THEN its baseUrl equals BASE_URL`() {
        val expectedBaseUrl = "https://fakestoreapi.com/"
        val actualBaseUrl = NetworkClient.retrofit.baseUrl().toString()
        assertEquals(expectedBaseUrl, actualBaseUrl)
    }
}
