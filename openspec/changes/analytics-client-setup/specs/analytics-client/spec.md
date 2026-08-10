## ADDED Requirements

### Requirement: AnalyticsProvider contract
`:core` SHALL define an `AnalyticsProvider` interface with `fun logEvent(name: String, params: Map<String, Any> = emptyMap())`, representing a single analytics SDK adapter. Any concrete provider (debug, or a future production SDK adapter) SHALL implement this interface.

#### Scenario: Provider is invoked with the event name and params
- **GIVEN** a class implementing `AnalyticsProvider`
- **WHEN** `logEvent("screen_view", mapOf("screen" to "home"))` is called on it
- **THEN** the provider's `logEvent` implementation receives `name = "screen_view"` and `params = mapOf("screen" to "home")`

#### Scenario: Params default to an empty map
- **GIVEN** a class implementing `AnalyticsProvider`
- **WHEN** `logEvent("app_open")` is called without a `params` argument
- **THEN** the provider's `logEvent` implementation receives `params = emptyMap()`

### Requirement: AnalyticsClient manages a thread-safe provider registry
`:core` SHALL define an `AnalyticsClient` class holding a registry of registered `AnalyticsProvider` instances backed by a `CopyOnWriteArrayList`, exposing `fun register(provider: AnalyticsProvider)` to add a provider to the registry. The registry SHALL support safe concurrent registration and event dispatch.

#### Scenario: Registering a provider adds it to the registry
- **GIVEN** a newly constructed `AnalyticsClient` with an empty registry
- **WHEN** `register(provider)` is called with an `AnalyticsProvider` instance
- **THEN** that provider is present in the client's registry and receives subsequent `logEvent()` calls

### Requirement: AnalyticsClient fans out events to every registered provider
`AnalyticsClient.logEvent(name, params)` SHALL forward the call to every currently registered `AnalyticsProvider` by invoking each provider's `logEvent(name, params)` with the same arguments.

#### Scenario: A single registered provider receives the event
- **GIVEN** an `AnalyticsClient` with exactly one registered `AnalyticsProvider`
- **WHEN** `analyticsClient.logEvent("purchase", mapOf("item_id" to "42"))` is called
- **THEN** the registered provider's `logEvent` is invoked exactly once with `name = "purchase"` and `params = mapOf("item_id" to "42")`

#### Scenario: Multiple registered providers all receive the event
- **GIVEN** an `AnalyticsClient` with three registered `AnalyticsProvider` instances
- **WHEN** `analyticsClient.logEvent("purchase")` is called
- **THEN** all three registered providers' `logEvent` methods are invoked exactly once with `name = "purchase"`

### Requirement: AnalyticsClient is a silent no-op with no registered providers
When `AnalyticsClient`'s registry is empty, calling `logEvent(name, params)` SHALL complete without invoking any provider and without throwing an exception. No separate no-op provider implementation exists; the empty registry itself is the no-op path.

#### Scenario: Logging an event with no registered providers does nothing
- **GIVEN** a newly constructed `AnalyticsClient` with no providers registered
- **WHEN** `analyticsClient.logEvent("app_open")` is called
- **THEN** the call completes without throwing and no provider receives any invocation

### Requirement: DebugAnalyticsProvider logs events to Logcat at DEBUG level
`:core` SHALL provide `DebugAnalyticsProvider`, an `AnalyticsProvider` implementation that logs each event to Android Logcat at `DEBUG` level, including the event name and its parameters in the logged message.

#### Scenario: Logging an event writes a DEBUG-level Logcat entry
- **GIVEN** a `DebugAnalyticsProvider` instance
- **WHEN** `logEvent("screen_view", mapOf("screen" to "home"))` is called
- **THEN** `Log.d` is invoked with a tag identifying the analytics debug provider and a message that includes the event name `"screen_view"` and the params `mapOf("screen" to "home")`

#### Scenario: Logging an event with empty params still logs the event name
- **GIVEN** a `DebugAnalyticsProvider` instance
- **WHEN** `logEvent("app_open")` is called with no params
- **THEN** `Log.d` is invoked with a message that includes the event name `"app_open"` and reflects an empty params map
