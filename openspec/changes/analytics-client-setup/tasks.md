## 1. Build Dependency (Prerequisite)

- [ ] 1.1 Add `testImplementation(libs.mockk)` to `core/build.gradle.kts`

## 2. AnalyticsProvider Contract (Prerequisite)

- [ ] 2.1 Define `core/src/main/kotlin/com/guidovezzoni/fakestore/core/analytics/AnalyticsProvider.kt` — interface with `fun logEvent(name: String, params: Map<String, Any> = emptyMap())`

## 3. AnalyticsClient Dispatcher (BDD)

- [ ] 3.1 Write test: GIVEN an `AnalyticsClient` with no registered providers WHEN `logEvent("app_open")` is called THEN the call completes without throwing and no provider receives any invocation, in `AnalyticsClientTest`
- [ ] 3.2 Write test: GIVEN an `AnalyticsClient` with exactly one registered `AnalyticsProvider` WHEN `logEvent("purchase", mapOf("item_id" to "42"))` is called THEN the registered provider's `logEvent` is invoked exactly once with `name = "purchase"` and `params = mapOf("item_id" to "42")`, in `AnalyticsClientTest`
- [ ] 3.3 Write test: GIVEN an `AnalyticsClient` with three registered `AnalyticsProvider` instances WHEN `logEvent("purchase")` is called THEN all three providers' `logEvent` methods are invoked exactly once with `name = "purchase"`, in `AnalyticsClientTest`
- [ ] 3.4 Implement: `core/src/main/kotlin/com/guidovezzoni/fakestore/core/analytics/AnalyticsClient.kt` — class holding a `CopyOnWriteArrayList<AnalyticsProvider>` registry, `fun register(provider: AnalyticsProvider)` to append to the registry, and `fun logEvent(name: String, params: Map<String, Any> = emptyMap())` that iterates the registry and forwards to each provider

## 4. DebugAnalyticsProvider (BDD)

- [ ] 4.1 Write test: GIVEN an event name and a non-empty params map WHEN the debug provider's log-message formatter formats them THEN the returned string includes both the event name and the params, in `DebugAnalyticsProviderTest`
- [ ] 4.2 Write test: GIVEN an event name and an empty params map WHEN the formatter formats them THEN the returned string reflects the empty params map, in `DebugAnalyticsProviderTest`
- [ ] 4.3 Implement: extracted pure formatter function (e.g. `internal fun formatLogMessage(name: String, params: Map<String, Any>): String`) in `core/src/main/kotlin/com/guidovezzoni/fakestore/core/analytics/DebugAnalyticsProvider.kt`
- [ ] 4.4 Write test: GIVEN a `DebugAnalyticsProvider` instance WHEN `logEvent("screen_view", mapOf("screen" to "home"))` is called THEN `Log.d` is invoked with the provider's log tag and the formatted message, verified via `mockkStatic(Log::class)`, in `DebugAnalyticsProviderTest`
- [ ] 4.5 Write test: GIVEN a `DebugAnalyticsProvider` instance WHEN `logEvent("app_open")` is called with no params THEN `Log.d` is invoked with a message reflecting the empty params map, verified via `mockkStatic(Log::class)`, in `DebugAnalyticsProviderTest`
- [ ] 4.6 Implement: `DebugAnalyticsProvider` class implementing `AnalyticsProvider`, calling `Log.d(TAG, formatLogMessage(name, params))` in `logEvent()`, with `TAG` extracted as a named `private const val`

## 5. Final Verification

- [ ] 5.1 Run `./gradlew detektDebug` and resolve any violations (extract `TAG` and any other literals to named constants)
- [ ] 5.2 Run `./gradlew test` and confirm all new tests pass
- [ ] 5.3 Run `./gradlew koverVerify` (or `koverHtmlReportDebug` for a detailed view) and confirm ≥95% coverage on the new `:core` analytics code
- [ ] 5.4 Cross-check the implementation against every Acceptance Criterion in the story: `AnalyticsProvider` interface exists, `AnalyticsClient` manages registered providers and distributes events, `logEvent()` forwards to every registered provider, `logEvent()` is a silent no-op with no providers registered, multiple providers all receive each event, the debug provider logs at Logcat `DEBUG` level, unit tests exist for both `AnalyticsClient` and the debug provider, and `./gradlew test` / `detektDebug` / `koverVerify` all pass
