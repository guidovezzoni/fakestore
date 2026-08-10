## Context

`:core` currently contains a single object, `NetworkClient`, and has one test file (`NetworkClientTest`, JUnit 4 only — MockK is not yet a `:core` test dependency, though it is already declared in `gradle/libs.versions.toml` at `1.13.13`). No other module (`:domain`, `:data`, `:app`) references analytics today, and `:app` has no DI container yet (no Hilt, no `di/` folder). This story is purely additive within `:core`; the design must anticipate story 1.1.3 (DI wiring) without doing that wiring itself.

The story text mandates support for **multiple simultaneously-registered SDKs** fanning out from one call site, a specific thread-safety mechanism (`CopyOnWriteArrayList`), no separate no-op class, and dual testing strategy for the Logcat provider (pure formatter + `mockkStatic(Log::class)`). These are user-confirmed and treated as fixed constraints rather than open decisions.

## Goals / Non-Goals

**Goals:**
- Provide a single `AnalyticsClient.logEvent(name, params)` call site that feature modules will use (from 1.1.3 onward) without depending on any concrete SDK.
- Support zero, one, or many registered `AnalyticsProvider` instances, with every registered provider receiving every event.
- Guarantee registry mutation (`register`) and iteration (`logEvent` fan-out) are thread-safe under concurrent access, per the read-heavy/write-rare `CopyOnWriteArrayList` decision.
- Ship one concrete, testable provider (`DebugAnalyticsProvider`) that proves the abstraction end-to-end via Logcat.
- Reach 95%+ Kover coverage on all new `:core` analytics code, including the `Log.d()` call path (via `mockkStatic`).

**Non-Goals:**
- No default/automatic registration of `DebugAnalyticsProvider` into `AnalyticsClient` — that is DI wiring, deferred to story 1.1.3.
- No production analytics SDK adapters (Firebase, Amplitude, etc.) — only the interface, dispatcher, and debug provider.
- No event-name or parameter validation/schema — `logEvent(name, params)` accepts any `String` name and `Map<String, Any>` params as given.
- No batching, retry, offline queuing, or sampling logic in `AnalyticsClient` — it is a straight synchronous fan-out.
- No changes to `:domain`, `:data`, `:app`, or the version catalog beyond the one `:core` test-dependency addition.

## Decisions

### 1. `AnalyticsProvider` as a plain interface, `AnalyticsClient` as a plain class
Both live directly under `core/.../analytics/`, following the existing `core/.../network/NetworkClient.kt` precedent (a plain Kotlin type, not an `object`, since `AnalyticsClient` now holds mutable state — the provider registry — and needs to be instantiable per-DI-graph in 1.1.3). `AnalyticsProvider` is a minimal single-method interface so any SDK adapter (including third-party wrapper classes) can implement it without extending a base class.

*Alternative considered*: A sealed/abstract base class instead of an interface. Rejected — an interface keeps SDK adapters free to extend their own vendor base classes if needed, and the contract is a single method with a default parameter, which Kotlin interfaces support natively.

### 2. No separate `NoOpAnalyticsProvider`
User-confirmed. `AnalyticsClient.logEvent()` iterates the (possibly empty) `CopyOnWriteArrayList`; an empty list produces zero iterations, which is a natural, allocation-free no-op. Introducing a `NoOpAnalyticsProvider` class would add a type with no behaviour to test beyond "does nothing," which is redundant with simply testing the empty-registry case on `AnalyticsClient` itself.

*Alternative considered*: `NoOpAnalyticsProvider : AnalyticsProvider` registered by default. Rejected per user clarification — adds a class whose only job is to do nothing, when the empty registry already achieves that.

### 3. Thread safety via `CopyOnWriteArrayList`
User-confirmed. Analytics registration happens once (or a handful of times) at DI-graph construction time in 1.1.3, while `logEvent()` fan-out happens frequently and potentially from multiple threads (background work triggering analytics, UI thread events). `CopyOnWriteArrayList` optimises exactly for this read-heavy/write-rare pattern: `add()` copies the backing array (cheap at registration-time scale — at most a handful of providers) and iteration never throws `ConcurrentModificationException`, so `logEvent()` can safely fan out while a `register()` call is in flight on another thread.

*Alternative considered*: `synchronized` block around a plain `MutableList`. Rejected — coarser-grained locking on every `logEvent()` call would serialise all event logging across threads, which is worse for the actual access pattern (many concurrent reads, rare writes).

### 4. Dual testing strategy for `DebugAnalyticsProvider`
User-confirmed. The provider extracts a pure formatter function — e.g. `internal fun formatLogMessage(name: String, params: Map<String, Any>): String` — into the same file (or a small internal companion), which is tested directly with plain JUnit assertions (no mocking needed for string-formatting logic). `logEvent()` itself is verified with `mockkStatic(Log::class)` to assert `Log.d(TAG, formattedMessage)` is invoked with the expected tag and message, without producing real Logcat output during test runs.

*Alternative considered*: Testing only via `mockkStatic(Log::class)` and asserting on the full formatted string in one place. Rejected — mixing pure-logic assertions into a mocked-static test makes failures harder to diagnose (is the formatting wrong, or the `Log.d` call wrong?); separating them isolates each concern and keeps the mockkStatic test focused on "was Log.d called correctly."

### 5. `core/build.gradle.kts` gains `testImplementation(libs.mockk)`
MockK `1.13.13` is already in `gradle/libs.versions.toml` (added in story 1.1.1 for `:domain`/`:data`) but not yet wired as a `:core` test dependency. This story adds the one missing `testImplementation(libs.mockk)` line to `core/build.gradle.kts`; no version catalog change is needed.

## Risks / Trade-offs

- **[Risk]** `mockkStatic(Log::class)` requires the `android.util.Log` class to be present on the test classpath with a mockable signature; `:core` is an Android library module (`fakestore.android.library`), so unit tests run against the unit-test variant of the Android SDK where `Log` methods normally throw `RuntimeException("not mocked")` unless intercepted. → **Mitigation**: MockK's `mockkStatic(Log::class)` intercepts calls to `Log.d(...)` before they reach the unmocked stub, so no `Robolectric` or `testOptions { unitTests.isReturnDefaultValues = true }` is required; confirm this in the first test run and fall back to `isReturnDefaultValues = true` in `core/build.gradle.kts` only if `mockkStatic` alone proves insufficient.
- **[Risk]** `CopyOnWriteArrayList.add()` under heavy concurrent registration could be O(n) per call, but registration is expected to happen a handful of times at app-startup DI-graph construction, not in a hot loop. → **Mitigation**: Documented as an accepted trade-off; if a future story registers providers dynamically at high frequency, this decision should be revisited.
- **[Trade-off]** `logEvent()` fans out synchronously on the caller's thread with no per-provider exception isolation — if one provider's `logEvent()` throws, it could prevent subsequent providers in the list from receiving the event. → Accepted for this story as the interface doesn't yet specify error-handling semantics; noted here so a future story can decide whether to wrap each provider call in a try/catch if a misbehaving SDK adapter becomes a problem in practice.

## Migration Plan

Not applicable — new, additive code with zero existing consumers. `:core`'s public API grows (three new types) but nothing existing depends on the `analytics` package yet, so this ships with zero behavioural change to the shipping app. Sequencing in `tasks.md`: build dependency (MockK) first, then `AnalyticsProvider` contract, then `AnalyticsClient` dispatcher (BDD), then `DebugAnalyticsProvider` (BDD), then final verification.

## Open Questions

None outstanding — thread-safety mechanism, no-op strategy, testing approach for the Logcat provider, and default-registration scope were all resolved via user clarification before this design was written.
