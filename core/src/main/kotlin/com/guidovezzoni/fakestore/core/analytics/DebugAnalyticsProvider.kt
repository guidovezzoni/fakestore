package com.guidovezzoni.fakestore.core.analytics

import android.util.Log

private const val TAG = "DebugAnalyticsProvider"

internal fun formatLogMessage(name: String, params: Map<String, Any>): String =
    "event=$name, params=$params"

class DebugAnalyticsProvider : AnalyticsProvider {

    override fun logEvent(name: String, params: Map<String, Any>) {
        Log.d(TAG, formatLogMessage(name, params))
    }
}
