## Why

Feature modules need to log analytics events without knowing which analytics SDK (or SDKs) are behind them. Today `:core` only exposes `NetworkClient`; there is no shared abstraction for event logging, so any future analytics integration would either hard-wire a specific vendor SDK into feature code or duplicate dispatch logic across modules. This story establishes that abstraction once, in `:core`, so downstream stories (starting with DI wiring in 1.1.3) can register one or more concrete providers without touching call sites.

## What Changes

- Add `AnalyticsProvider` interface in `:core` with `fun logEvent(name: String, params: Map<String, Any> = emptyMap())` — the contract every analytics SDK adapter implements.
- Add `AnalyticsClient` class in `:core` that holds a thread-safe registry of `AnalyticsProvider` instances (`CopyOnWriteArrayList`), exposes `register(provider: AnalyticsProvider)`, and fans out `logEvent()` calls to every registered provider.
- `AnalyticsClient.logEvent()` is a silent no-op when the registry is empty — no separate `NoOpAnalyticsProvider` class is introduced.
- Add `DebugAnalyticsProvider` — a Logcat-based `AnalyticsProvider` implementation that logs events at `DEBUG` level, with a pure formatter function extracted for unit testing plus `Log.d()` verified via `mockkStatic(Log::class)`.
- Add `testImplementation(libs.mockk)` to `core/build.gradle.kts` (MockK is not yet a `:core` test dependency).
- Unit tests for `AnalyticsClient` (fan-out to one/many providers, empty-registry no-op) and `DebugAnalyticsProvider` (formatter logic, Logcat call verification).
- No changes to `:domain`, `:data`, `:app`, `settings.gradle.kts`, or the version catalog beyond the `:core` test dependency above. Default provider registration (wiring `DebugAnalyticsProvider` into `AnalyticsClient` via DI) is explicitly deferred to story 1.1.3.

## Capabilities

### New Capabilities
- `analytics-client`: The `:core`-level analytics abstraction — the `AnalyticsProvider` contract, the thread-safe `AnalyticsClient` dispatcher that fans out events to all registered providers (silent no-op when none are registered), and the `DebugAnalyticsProvider` Logcat implementation.

### Modified Capabilities
- None. This is a greenfield addition to `:core`; no existing specs are affected.

## Impact

- **Modified build files**: `core/build.gradle.kts` (add `testImplementation(libs.mockk)`). No other Gradle files change.
- **New source code**: `core/src/main/kotlin/com/guidovezzoni/fakestore/core/analytics/AnalyticsProvider.kt`, `AnalyticsClient.kt`, `DebugAnalyticsProvider.kt` (and a small extracted formatter for the debug provider).
- **New tests**: `core/src/test/kotlin/com/guidovezzoni/fakestore/core/analytics/AnalyticsClientTest.kt`, `DebugAnalyticsProviderTest.kt`.
- **No UI, no DI wiring** — registering `DebugAnalyticsProvider` with `AnalyticsClient` by default is deferred to story 1.1.3.
- **No breaking changes** — nothing existing depends on `:core`'s analytics package yet.
