## ADDED Requirements

### Requirement: ProductListViewModel logs product_list_viewed exactly once per successful visit
`ProductListViewModel` SHALL take an `AnalyticsClient` constructor dependency. On every successful fetch — i.e. every time `getProductsUseCase()`'s `Result.onSuccess` branch runs within `loadProducts()`, transitioning `uiState` to `ProductListUiState.Content` (populated or empty) — it SHALL call `AnalyticsClient.logEvent("product_list_viewed")` with an empty parameters map, exactly once per successful fetch. It SHALL NOT call `logEvent("product_list_viewed")` when a fetch fails (`uiState` transitions to `ProductListUiState.Error`), and SHALL NOT call it at any other time (e.g. merely because `uiState` is read while already `Content`, with no new fetch dispatched).

#### Scenario: A successful fetch with populated products logs product_list_viewed once
- **GIVEN** a `ProductListViewModel` constructed with a mocked `GetProductsUseCase` returning `Result.success` with a non-empty product list, and a mocked `AnalyticsClient`
- **WHEN** `ProductListUiIntent.LoadProducts` is dispatched via `onIntent()`
- **THEN** the mocked `AnalyticsClient.logEvent()` is invoked exactly once with `name = "product_list_viewed"` and empty `params`

#### Scenario: A successful fetch with an empty product list still logs product_list_viewed
- **GIVEN** a `ProductListViewModel` constructed with a mocked `GetProductsUseCase` returning `Result.success` with an empty product list, and a mocked `AnalyticsClient`
- **WHEN** `ProductListUiIntent.LoadProducts` is dispatched via `onIntent()`
- **THEN** the mocked `AnalyticsClient.logEvent()` is invoked exactly once with `name = "product_list_viewed"` and empty `params`

#### Scenario: A failed fetch does not log product_list_viewed
- **GIVEN** a `ProductListViewModel` constructed with a mocked `GetProductsUseCase` returning `Result.failure`, and a mocked `AnalyticsClient`
- **WHEN** `ProductListUiIntent.LoadProducts` is dispatched via `onIntent()`
- **THEN** the mocked `AnalyticsClient.logEvent()` is never invoked with `name = "product_list_viewed"`

#### Scenario: Each successful LoadProducts dispatch logs a separate visit
- **GIVEN** a `ProductListViewModel` constructed with a mocked `GetProductsUseCase` returning `Result.success` with a fixed product list on every invocation, and a mocked `AnalyticsClient`
- **WHEN** `ProductListUiIntent.LoadProducts` is dispatched twice via `onIntent()`
- **THEN** the mocked `AnalyticsClient.logEvent()` is invoked exactly twice with `name = "product_list_viewed"`, once per dispatch

#### Scenario: A failed fetch followed by a successful retry logs product_list_viewed exactly once
- **GIVEN** a `ProductListViewModel` constructed with a mocked `GetProductsUseCase` that returns `Result.failure` on its first invocation and `Result.success` with a product list on its second invocation, and a mocked `AnalyticsClient`
- **WHEN** `ProductListUiIntent.LoadProducts` is dispatched followed by `ProductListUiIntent.RetryClicked`
- **THEN** the mocked `AnalyticsClient.logEvent()` is invoked exactly once with `name = "product_list_viewed"`, corresponding to the successful retry
