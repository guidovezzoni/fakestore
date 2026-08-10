package com.guidovezzoni.fakestore.core.analytics

import android.util.Log

private const val TAG = "DebugAnalyticsProvider"

internal fun formatLogMessage(name: String, params: Map<String, Any>): String =
    "event=$name, params=$params"

/**
 * Debug [AnalyticsProvider] that writes every event to Logcat at DEBUG level.
 *
 * Register with [AnalyticsClient] in debug build variants only. Never register in release
 * builds: Logcat output is accessible via ADB on connected devices.
 *
 * Thread-safe: delegates to [android.util.Log.d], which is thread-safe.
 */
class DebugAnalyticsProvider : AnalyticsProvider {

    override fun logEvent(name: String, params: Map<String, Any>) {
        Log.d(TAG, formatLogMessage(name, params))
    }
}
