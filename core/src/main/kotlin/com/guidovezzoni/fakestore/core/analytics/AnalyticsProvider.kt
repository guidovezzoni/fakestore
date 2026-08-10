package com.guidovezzoni.fakestore.core.analytics

interface AnalyticsProvider {
    fun logEvent(name: String, params: Map<String, Any> = emptyMap())
}
