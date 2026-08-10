package com.guidovezzoni.fakestore.core.analytics

/**
 * Contract for an analytics backend (e.g. Firebase, Mixpanel, Logcat).
 *
 * Register implementations with [AnalyticsClient]; feature modules never depend on this
 * interface directly — they call [AnalyticsClient.logEvent] instead.
 *
 * Implementations must be thread-safe: [logEvent] may be called from any thread or
 * coroutine context.
 *
 * **Parameter constraints**
 * - [name] must follow `snake_case` convention (e.g. `product_list_viewed`).
 * - [params] values must be primitives: [String], [Int], [Long], [Double], or [Boolean].
 *   Never include PII (names, emails, addresses) or credentials (tokens, passwords).
 */
interface AnalyticsProvider {
    fun logEvent(name: String, params: Map<String, Any> = emptyMap())
}
