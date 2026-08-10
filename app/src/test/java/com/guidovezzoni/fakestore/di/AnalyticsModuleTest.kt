package com.guidovezzoni.fakestore.di

import com.guidovezzoni.fakestore.core.analytics.AnalyticsClient
import com.guidovezzoni.fakestore.core.analytics.AnalyticsProvider
import com.guidovezzoni.fakestore.core.analytics.DebugAnalyticsProvider
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class AnalyticsModuleTest {

    @MockK(relaxed = true)
    private lateinit var mockProvider: AnalyticsProvider

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `GIVEN the analytics provisioning function is invoked with isDebug true WHEN the registered providers are inspected THEN one DebugAnalyticsProvider is registered reflecting one active debug provider`() {
        val analyticsClient = provideAnalyticsClient(isDebug = true)

        val providers = analyticsClient.registeredProviders()

        val expectedProviderCount = 1
        assertEquals(expectedProviderCount, providers.size)
        assertTrue(providers[0] is DebugAnalyticsProvider)
    }

    @Test
    fun `GIVEN the analytics provisioning function is invoked with isDebug false WHEN the resulting AnalyticsClient logEvent app_open is called THEN no debug provider is registered and no debug-only side effect occurs`() {
        val analyticsClient = provideAnalyticsClient(isDebug = false)
        analyticsClient.register(mockProvider)

        analyticsClient.logEvent("app_open")

        verify(exactly = 1) { mockProvider.logEvent("app_open", emptyMap()) }
    }

    private fun AnalyticsClient.registeredProviders(): List<AnalyticsProvider> {
        val providersField = AnalyticsClient::class.java.getDeclaredField("providers")
        providersField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return (providersField.get(this) as CopyOnWriteArrayList<AnalyticsProvider>).toList()
    }
}
