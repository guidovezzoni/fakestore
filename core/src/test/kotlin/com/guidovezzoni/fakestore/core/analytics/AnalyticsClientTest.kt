package com.guidovezzoni.fakestore.core.analytics

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class AnalyticsClientTest {

    @MockK(relaxed = true)
    private lateinit var providerOne: AnalyticsProvider

    @MockK(relaxed = true)
    private lateinit var providerTwo: AnalyticsProvider

    @MockK(relaxed = true)
    private lateinit var providerThree: AnalyticsProvider

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `GIVEN an AnalyticsClient with no registered providers WHEN logEvent app_open is called THEN the call completes without throwing and no provider receives any invocation`() {
        val analyticsClient = AnalyticsClient()

        analyticsClient.logEvent("app_open")

        verify(exactly = 0) { providerOne.logEvent(any(), any()) }
        verify(exactly = 0) { providerTwo.logEvent(any(), any()) }
        verify(exactly = 0) { providerThree.logEvent(any(), any()) }
    }

    @Test
    fun `GIVEN an AnalyticsClient with exactly one registered AnalyticsProvider WHEN logEvent purchase is called THEN the registered provider logEvent is invoked exactly once with name purchase and the given params`() {
        val analyticsClient = AnalyticsClient()
        analyticsClient.register(providerOne)

        val expectedName = "purchase"
        val expectedParams = mapOf("item_id" to "42")

        analyticsClient.logEvent(expectedName, expectedParams)

        verify(exactly = 1) { providerOne.logEvent(expectedName, expectedParams) }
    }

    @Test
    fun `GIVEN an AnalyticsClient with three registered AnalyticsProvider instances WHEN logEvent purchase is called THEN all three providers logEvent methods are invoked exactly once with name purchase`() {
        val analyticsClient = AnalyticsClient()
        analyticsClient.register(providerOne)
        analyticsClient.register(providerTwo)
        analyticsClient.register(providerThree)

        val expectedName = "purchase"

        analyticsClient.logEvent(expectedName)

        verify(exactly = 1) { providerOne.logEvent(expectedName, any()) }
        verify(exactly = 1) { providerTwo.logEvent(expectedName, any()) }
        verify(exactly = 1) { providerThree.logEvent(expectedName, any()) }
    }
}
