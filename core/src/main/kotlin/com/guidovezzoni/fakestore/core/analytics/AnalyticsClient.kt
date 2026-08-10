package com.guidovezzoni.fakestore.core.analytics

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Central analytics dispatcher. Feature modules call [logEvent] here; the event is forwarded
 * to every registered [AnalyticsProvider].
 *
 * **Usage**
 * 1. Register one or more [AnalyticsProvider] implementations via [register] at app start-up.
 * 2. Call [logEvent] from any feature module using `snake_case` event names.
 *
 * If no providers are registered, [logEvent] is a silent no-op.
 *
 * **Thread safety**: [register] and [logEvent] are safe to call concurrently from any thread
 * or coroutine context. The provider list is backed by [CopyOnWriteArrayList].
 *
 * **Parameter constraints**: [params] values must be primitives ([String], [Int], [Long],
 * [Double], [Boolean]). Never include PII or credentials.
 */
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
