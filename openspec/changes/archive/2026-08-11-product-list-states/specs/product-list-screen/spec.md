# Capability: Product List Screen (Delta)

## MODIFIED Requirements

### Requirement: ProductListUiState is a sealed type with Loading, Content, and Error variants
`:app` SHALL define `ProductListUiState` as a `sealed interface` with exactly three variants: `Loading` (a `data object` with no fields), `Content` (a `data class` with a single field `products: List<ProductListItem>`), and `Error` (a `data object` with no fields). No combination of loading, content, and error status other than one of these three variants SHALL be representable.

#### Scenario: Loading is a data object with no fields
- **WHEN** `ProductListUiState.Loading` is referenced
- **THEN** it is a `data object` member of the `ProductListUiState` sealed interface, carrying no product data

#### Scenario: Content holds the display-ready product list
- **GIVEN** a list of `ProductListItem` values
- **WHEN** `ProductListUiState.Content(products = list)` is constructed
- **THEN** `products` equals the given list, and no other field is present

#### Scenario: Error is a data object with no fields
- **WHEN** `ProductListUiState.Error` is referenced
- **THEN** it is a `data object` member of the `ProductListUiState` sealed interface, carrying no error message or other data

### Requirement: ProductListUiIntent models the initial load and retry actions
`:app` SHALL define `ProductListUiIntent` as a sealed interface with two entries: `LoadProducts` (triggered on the screen's first composition) and `RetryClicked` (triggered when the user taps the retry button in the error state). Both SHALL route to the same fetch behaviour in `ProductListViewModel`.

#### Scenario: LoadProducts is a distinct sealed subtype
- **WHEN** `ProductListUiIntent.LoadProducts` is referenced
- **THEN** it is a member of the `ProductListUiIntent` sealed interface, usable in an exhaustive `when` block over `ProductListViewModel.onIntent()`

#### Scenario: RetryClicked is a distinct sealed entry
- **WHEN** `ProductListUiIntent.RetryClicked` is referenced
- **THEN** it is a member of the `ProductListUiIntent` sealed interface, distinct from `LoadProducts`, usable in an exhaustive `when` block over `ProductListViewModel.onIntent()`

### Requirement: ProductListViewModel emits Loading, Content, or Error for every fetch
`ProductListViewModel` SHALL initialise `uiState` to `ProductListUiState.Loading`. On receiving `ProductListUiIntent.LoadProducts` or `ProductListUiIntent.RetryClicked` via `onIntent()`, it SHALL first set `uiState` to `ProductListUiState.Loading`, then invoke `GetProductsUseCase()`; on `Result.success`, it SHALL map each domain `Product` to a `ProductListItem` (preserving order, including when the list is empty) and set `uiState` to `ProductListUiState.Content` with the mapped list; on `Result.failure`, it SHALL set `uiState` to `ProductListUiState.Error`, without deriving any part of the state from the failure's exception (message, class name, or stack trace).

#### Scenario: Initial state is Loading before any intent is dispatched
- **GIVEN** a newly constructed `ProductListViewModel`
- **WHEN** no intent has been dispatched
- **THEN** `uiState.value` is `ProductListUiState.Loading`

#### Scenario: LoadProducts populates uiState with mapped Content on success
- **GIVEN** a mocked `GetProductsUseCase` whose `invoke()` returns a `Flow` emitting `Result.success` with a list of `Product` domain models
- **WHEN** `ProductListUiIntent.LoadProducts` is dispatched via `onIntent()` and collection completes
- **THEN** `uiState.value` is `ProductListUiState.Content` containing one `ProductListItem` per input `Product`, in the same order, with `id` equal to the source `Product.id`

#### Scenario: An empty successful response yields Content with an empty list
- **GIVEN** a mocked `GetProductsUseCase` whose `invoke()` returns a `Flow` emitting `Result.success` with an empty list
- **WHEN** `ProductListUiIntent.LoadProducts` is dispatched via `onIntent()`
- **THEN** `uiState.value` equals `ProductListUiState.Content(products = emptyList())`

#### Scenario: A failed fetch yields Error with no exception detail
- **GIVEN** a mocked `GetProductsUseCase` whose `invoke()` returns a `Flow` emitting `Result.failure` with a technical exception
- **WHEN** `ProductListUiIntent.LoadProducts` is dispatched via `onIntent()`
- **THEN** `uiState.value` is `ProductListUiState.Error`, a data object that structurally cannot carry the exception's message, class name, or stack trace

#### Scenario: RetryClicked re-triggers the fetch and can recover from Error
- **GIVEN** a mocked `GetProductsUseCase` that returns `Result.failure` on its first invocation and `Result.success` with a product list on its second invocation
- **WHEN** `ProductListUiIntent.LoadProducts` is dispatched followed by `ProductListUiIntent.RetryClicked`
- **THEN** `uiState.value` transitions from `ProductListUiState.Error` to `ProductListUiState.Content` containing the mapped products from the second invocation

#### Scenario: Repeated LoadProducts intents do not corrupt state
- **GIVEN** a mocked `GetProductsUseCase` returning a fixed successful product list
- **WHEN** `ProductListUiIntent.LoadProducts` is dispatched twice via `onIntent()`
- **THEN** `uiState.value` still equals the mapped `Content` list, with no duplicated entries

### Requirement: ProductListScreen renders state-specific UI with the top app bar always visible
`:app`'s stateless `ProductListScreen(uiState: ProductListUiState, onIntent: (ProductListUiIntent) -> Unit, modifier: Modifier = Modifier)` composable SHALL render its `Scaffold`'s `topBar` unconditionally across all `ProductListUiState` variants, and SHALL branch its body on `uiState`: a centred loading indicator for `Loading`; for `Content`, either the existing `LazyColumn` of `ProductListItemCard`s (keyed by `id`) when `products` is non-empty, or a distinct empty-state message when `products` is empty; and for `Error`, a user-facing error message together with a "Retry" button that, when tapped, dispatches `ProductListUiIntent.RetryClicked`.

#### Scenario: Loading state shows only the loading indicator
- **GIVEN** `uiState` is `ProductListUiState.Loading`
- **WHEN** `ProductListScreen(uiState, onIntent)` is composed
- **THEN** a loading indicator is displayed and no product cards or other content are visible

#### Scenario: Content with products shows the product list
- **GIVEN** `uiState` is `ProductListUiState.Content` with a non-empty `products` list
- **WHEN** `ProductListScreen(uiState, onIntent)` is composed
- **THEN** a `LazyColumn` is displayed containing one card per item in `products`, each keyed by the item's `id`

#### Scenario: Content with an empty list shows the empty-state message
- **GIVEN** `uiState` is `ProductListUiState.Content(products = emptyList())`
- **WHEN** `ProductListScreen(uiState, onIntent)` is composed
- **THEN** an empty-state message is displayed and no product cards are visible

#### Scenario: Error state shows the error message and a Retry button
- **GIVEN** `uiState` is `ProductListUiState.Error`
- **WHEN** `ProductListScreen(uiState, onIntent)` is composed
- **THEN** a user-facing error message is displayed together with a "Retry" button, and neither the product list nor the loading indicator is visible

#### Scenario: Tapping Retry dispatches RetryClicked
- **GIVEN** `uiState` is `ProductListUiState.Error`
- **WHEN** the user taps the "Retry" button
- **THEN** `ProductListUiIntent.RetryClicked` is dispatched to `onIntent`

#### Scenario: LoadProducts intent fires on first composition
- **GIVEN** the stateful `ProductListScreen()` overload is composed for the first time
- **WHEN** composition completes
- **THEN** `ProductListUiIntent.LoadProducts` has been dispatched to the underlying `ProductListViewModel.onIntent()` exactly once

#### Scenario: The top app bar is visible in every state
- **GIVEN** `uiState` is, in turn, `ProductListUiState.Loading`, `ProductListUiState.Content` (populated), `ProductListUiState.Content` (empty), and `ProductListUiState.Error`
- **WHEN** `ProductListScreen(uiState, onIntent)` is composed for each
- **THEN** the top app bar with the screen title is displayed in every case

## ADDED Requirements

### Requirement: Error, retry, and empty-state strings are localised in English and Spanish
`app/src/main/res/values/strings.xml` SHALL define `product_list_error_message`, `product_list_retry_button`, and `product_list_empty_message` as English (base) string resources, each with a corresponding translated entry in `app/src/main/res/values-es/strings.xml`, and none of the three SHALL appear as a hardcoded literal in `ProductListScreen.kt`.

#### Scenario: Every new string resource has a Spanish translation
- **WHEN** `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml` are compared
- **THEN** `product_list_error_message`, `product_list_retry_button`, and `product_list_empty_message` each exist in both files with a non-empty, distinct Spanish value in `values-es`

#### Scenario: No hardcoded literals for the new user-facing strings
- **WHEN** `ProductListScreen.kt` is inspected
- **THEN** the error message, retry button label, and empty-state message are each sourced via `stringResource(R.string.*)`, with no inline string literals
