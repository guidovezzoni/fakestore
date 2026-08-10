package com.guidovezzoni.fakestore.core.analytics

import java.util.concurrent.CopyOnWriteArrayList

class AnalyticsClient {

    private val providers = CopyOnWriteArrayList<AnalyticsProvider>()

    fun register(provider: AnalyticsProvider) {
        providers.add(provider)
    }

    fun logEvent(name: String, params: Map<String, Any> = emptyMap()) {
        providers.forEach { provider ->
            provider.logEvent(name, params)
        }
    }
}
