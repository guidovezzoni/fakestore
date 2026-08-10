package com.guidovezzoni.fakestore.di

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkModuleTest {
    @Test
    fun `GIVEN NetworkModule provideNetworkClient and provideRetrofit are called directly WHEN the returned Retrofit base URL is inspected THEN it equals https fakestoreapi com`() {
        val expectedBaseUrl = "https://fakestoreapi.com/"
        val networkClient = NetworkModule.provideNetworkClient()
        val retrofit = NetworkModule.provideRetrofit(networkClient)
        val actualBaseUrl = retrofit.baseUrl().toString()
        assertEquals(expectedBaseUrl, actualBaseUrl)
    }
}
