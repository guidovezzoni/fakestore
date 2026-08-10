package com.guidovezzoni.fakestore.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkClientTest {
    @Test
    fun `GIVEN a newly constructed NetworkClient instance WHEN its retrofit property's base URL is inspected THEN it equals https fakestoreapi com`() {
        val expectedBaseUrl = "https://fakestoreapi.com/"
        val actualBaseUrl = NetworkClient().retrofit.baseUrl().toString()
        assertEquals(expectedBaseUrl, actualBaseUrl)
    }
}
