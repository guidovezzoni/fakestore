package com.guidovezzoni.fakestore.core.analytics

import android.util.Log
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DebugAnalyticsProviderTest {

    private lateinit var debugAnalyticsProvider: DebugAnalyticsProvider

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        debugAnalyticsProvider = DebugAnalyticsProvider()
    }

    @Test
    fun `GIVEN an event name and a non-empty params map WHEN the debug provider log-message formatter formats them THEN the returned string includes both the event name and the params`() {
        val eventName = "screen_view"
        val params = mapOf<String, Any>("screen" to "home")

        val result = formatLogMessage(eventName, params)

        assertTrue(result.contains(eventName))
        assertTrue(result.contains("screen"))
        assertTrue(result.contains("home"))
    }

    @Test
    fun `GIVEN an event name and an empty params map WHEN the formatter formats them THEN the returned string reflects the empty params map`() {
        val eventName = "app_open"
        val params = emptyMap<String, Any>()

        val result = formatLogMessage(eventName, params)

        val expectedEmptyParams = params.toString()
        assertTrue(result.contains(eventName))
        assertTrue(result.contains(expectedEmptyParams))
    }

    @Test
    fun `GIVEN a DebugAnalyticsProvider instance WHEN logEvent screen_view with params is called THEN Log d is invoked with the provider log tag and the formatted message`() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        val eventName = "screen_view"
        val params = mapOf<String, Any>("screen" to "home")
        val expectedTag = "DebugAnalyticsProvider"
        val expectedMessage = formatLogMessage(eventName, params)

        debugAnalyticsProvider.logEvent(eventName, params)

        verify(exactly = 1) { Log.d(expectedTag, expectedMessage) }
    }

    @Test
    fun `GIVEN a DebugAnalyticsProvider instance WHEN logEvent app_open is called with no params THEN Log d is invoked with a message reflecting the empty params map`() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        val eventName = "app_open"
        val expectedTag = "DebugAnalyticsProvider"
        val expectedMessage = formatLogMessage(eventName, emptyMap())

        debugAnalyticsProvider.logEvent(eventName)

        verify(exactly = 1) { Log.d(expectedTag, expectedMessage) }
    }
}
