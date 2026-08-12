# Capability: Product List Screen

## Purpose

Displays a scrollable list of products fetched from the Fake Store API, showing each product's image, title, price, and rating score. This is the app's initial screen shown on launch.

## Requirements

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

### Requirement: ProductListItem carries favourite state
`:app` SHALL extend `ProductListItem` with `isFavourite: Boolean` (default `false`), computed by `ProductListViewModel`/`FavouritesViewModel` when mapping a domain `Product`. The corresponding content description string is resolved in the composable via `stringResource()`, not stored on `ProductListItem`.

#### Scenario: A favourited product's item reflects isFavourite = true
- **GIVEN** a `Product` whose `id` is present in the current favourite ID set
- **WHEN** it is mapped to a `ProductListItem`
- **THEN** `isFavourite` equals `true`

#### Scenario: An unfavourited product's item reflects isFavourite = false
- **GIVEN** a `Product` whose `id` is absent from the current favourite ID set
- **WHEN** it is mapped to a `ProductListItem`
- **THEN** `isFavourite` equals `false`

### Requirement: ProductListUiIntent models the initial load, retry, and favourite toggle actions
`:app` SHALL define `ProductListUiIntent` as a sealed interface with three entries: `LoadProducts` (triggered on the screen's first composition), `RetryClicked` (triggered when the user taps the retry button in the error state), and `ToggleFavourite(val productId: Int)` (triggered when the user taps a product card's heart icon).

#### Scenario: LoadProducts is a distinct sealed subtype
- **WHEN** `ProductListUiIntent.LoadProducts` is referenced
- **THEN** it is a member of the `ProductListUiIntent` sealed interface, usable in an exhaustive `when` block over `ProductListViewModel.onIntent()`

#### Scenario: RetryClicked is a distinct sealed entry
- **WHEN** `ProductListUiIntent.RetryClicked` is referenced
- **THEN** it is a member of the `ProductListUiIntent` sealed interface, distinct from `LoadProducts`, usable in an exhaustive `when` block over `ProductListViewModel.onIntent()`

#### Scenario: ToggleFavourite carries the tapped product's ID
- **WHEN** `ProductListUiIntent.ToggleFavourite(productId = 7)` is constructed
- **THEN** its `productId` field equals `7`, and it is a member of the `ProductListUiIntent` sealed interface distinct from `LoadProducts` and `RetryClicked`

### Requirement: ProductListUiEffect models a favourite toggle failure notification
`:app` SHALL define `ProductListUiEffect` as a sealed interface with `ShowFavouriteToggleError`, emitted by `ProductListViewModel` when a `ToggleFavourite` intent's underlying write fails, and consumed exactly once by `ProductListScreen` to show a snackbar.

#### Scenario: ShowFavouriteToggleError is emitted on a failed toggle
- **GIVEN** a `ProductListViewModel` backed by a mocked `ToggleFavouriteUseCase` that returns `Result.failure(...)`
- **WHEN** `ProductListUiIntent.ToggleFavourite(productId = 7)` is dispatched
- **THEN** `uiEffect` emits `ProductListUiEffect.ShowFavouriteToggleError`

### Requirement: ProductListViewModel combines products with reactive favourite state and handles optimistic toggling
`ProductListViewModel` SHALL initialise `uiState` to `ProductListUiState.Loading`. On receiving `ProductListUiIntent.LoadProducts` or `ProductListUiIntent.RetryClicked` via `onIntent()`, it SHALL first set `uiState` to `ProductListUiState.Loading`, then invoke `GetProductsUseCase()`; on `Result.success`, it SHALL map each domain `Product` to a `ProductListItem` (preserving order, including when the list is empty) and set `uiState` to `ProductListUiState.Content` with the mapped list; on `Result.failure`, it SHALL set `uiState` to `ProductListUiState.Error`, without deriving any part of the state from the failure's exception (message, class name, or stack trace).

`ProductListViewModel` SHALL additionally be constructor-injected with `GetFavouriteIdsUseCase` and `ToggleFavouriteUseCase` (no `Context` dependency). On a successful product fetch, it SHALL `combine()` the fetched products with `GetFavouriteIdsUseCase()`'s reactive `Flow<Set<Int>>`, mapping each product to a `ProductListItem` with `isFavourite` set from set membership, and SHALL re-derive `ProductListUiState.Content` whenever the favourite ID set changes, without requiring a new `LoadProducts`/`RetryClicked` dispatch.

On `ProductListUiIntent.ToggleFavourite(productId)`, it SHALL immediately flip that item's `isFavourite` in the currently displayed `Content.products` (optimistic update), then call `ToggleFavouriteUseCase(productId, shouldBeFavourite = <the new state>)`. On success, it SHALL log `favourite_added` or `favourite_removed` (selected by the new state) with `params = mapOf("product_id" to productId)`. On failure, it SHALL restore the pre-toggle `products` list and emit `ProductListUiEffect.ShowFavouriteToggleError`; it SHALL NOT log any analytics event in this case.

The existing Loading/Content/Error emission behaviour on `LoadProducts`/`RetryClicked` (including empty-list handling and `product_list_viewed` logging) is unchanged by this requirement.

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

#### Scenario: LoadProducts populates uiState with favourite-aware Content on success
- **GIVEN** a mocked `GetProductsUseCase` returning products with ids `1` and `2`, and a mocked `GetFavouriteIdsUseCase` emitting `setOf(2)`
- **WHEN** `ProductListUiIntent.LoadProducts` is dispatched via `onIntent()`
- **THEN** `uiState.value` is `ProductListUiState.Content` where the item with `id = 2` has `isFavourite = true` and the item with `id = 1` has `isFavourite = false`

#### Scenario: A change to the favourites table updates Content without a new LoadProducts dispatch
- **GIVEN** `ProductListUiIntent.LoadProducts` has already been dispatched successfully
- **WHEN** the mocked `GetFavouriteIdsUseCase`'s underlying flow emits a new set including a previously-unfavourited product's id (simulating a toggle made from the Favourites screen)
- **THEN** `uiState.value`'s corresponding `ProductListItem.isFavourite` becomes `true`, with no new intent dispatched

#### Scenario: Tapping the heart icon optimistically flips isFavourite before the write completes
- **GIVEN** `uiState.value` is `ProductListUiState.Content` containing a product with `id = 7` and `isFavourite = false`
- **WHEN** `ProductListUiIntent.ToggleFavourite(productId = 7)` is dispatched, before the underlying write completes
- **THEN** `uiState.value`'s item with `id = 7` has `isFavourite = true`

#### Scenario: A successful toggle logs the correct analytics event
- **GIVEN** a mocked `ToggleFavouriteUseCase` that returns `Result.success(Unit)` and a mocked `AnalyticsClient`, and `uiState.value` containing a product with `id = 7` and `isFavourite = false`
- **WHEN** `ProductListUiIntent.ToggleFavourite(productId = 7)` is dispatched and the write completes
- **THEN** `AnalyticsClient.logEvent()` is invoked exactly once with `name = "favourite_added"` and `params = mapOf("product_id" to 7)`

#### Scenario: A failed toggle reverts the optimistic update and emits an effect without logging analytics
- **GIVEN** a mocked `ToggleFavouriteUseCase` that returns `Result.failure(...)`, a mocked `AnalyticsClient`, and `uiState.value` containing a product with `id = 7` and `isFavourite = false`
- **WHEN** `ProductListUiIntent.ToggleFavourite(productId = 7)` is dispatched and the write fails
- **THEN** `uiState.value`'s item with `id = 7` reverts to `isFavourite = false`, `uiEffect` emits `ProductListUiEffect.ShowFavouriteToggleError`, and `AnalyticsClient.logEvent()` is never invoked with `name = "favourite_added"` or `"favourite_removed"`

### Requirement: Price is formatted as locale-aware USD currency
`ProductListViewModel` SHALL format each `Product.price` into `ProductListItem.formattedPrice` using a locale-aware currency formatter whose currency is fixed to USD regardless of the device/test locale's own currency, so only the symbol, grouping, and decimal separator vary by locale.

#### Scenario: Price formats correctly for en-US locale
- **GIVEN** a `Product` with `price = 109.95` and formatting locale `en-US`
- **WHEN** `ProductListViewModel` maps the product to a `ProductListItem`
- **THEN** `formattedPrice` equals `"$109.95"`

#### Scenario: Price formats correctly for es-ES locale, remaining in USD
- **GIVEN** a `Product` with `price = 109.95` and formatting locale `es-ES`
- **WHEN** `ProductListViewModel` maps the product to a `ProductListItem`
- **THEN** `formattedPrice` is a USD-denominated string using Spanish grouping/decimal conventions (e.g. `"109,95 US$"`), not the Euro symbol

### Requirement: Rating score is formatted as a locale-aware number
`ProductListViewModel` SHALL format each `Product.rating.score` into `ProductListItem.formattedRatingScore` using a locale-aware number formatter, so the decimal separator follows the formatting locale.

#### Scenario: Rating score formats correctly for en-US locale
- **GIVEN** a `Product` with `rating.score = 4.1` and formatting locale `en-US`
- **WHEN** `ProductListViewModel` maps the product to a `ProductListItem`
- **THEN** `formattedRatingScore` equals `"4.1"`

#### Scenario: Rating score formats correctly for es-ES locale
- **GIVEN** a `Product` with `rating.score = 4.1` and formatting locale `es-ES`
- **WHEN** `ProductListViewModel` maps the product to a `ProductListItem`
- **THEN** `formattedRatingScore` equals `"4,1"`

### Requirement: ProductListScreen shows a snackbar when a favourite toggle fails
`:app`'s stateless `ProductListScreen(uiState: ProductListUiState, onIntent: (ProductListUiIntent) -> Unit, uiEffect: Flow<ProductListUiEffect> = emptyFlow(), modifier: Modifier = Modifier)` composable SHALL render its `Scaffold`'s `topBar` unconditionally across all `ProductListUiState` variants, and SHALL branch its body on `uiState`: a centred loading indicator for `Loading`; for `Content`, either the existing `LazyColumn` of `ProductListItemCard`s (keyed by `id`) when `products` is non-empty, or a distinct empty-state message when `products` is empty; and for `Error`, a user-facing error message together with a "Retry" button that, when tapped, dispatches `ProductListUiIntent.RetryClicked`. The stateless composable SHALL additionally collect `uiEffect` and, on `ProductListUiEffect.ShowFavouriteToggleError`, show a `Snackbar` (via a `Scaffold`'s `snackbarHost`) displaying the localised `favourite_toggle_error_message`. Each rendered `ProductListItemCard` SHALL be wired so that tapping its favourite icon dispatches `ProductListUiIntent.ToggleFavourite(item.id)` to `onIntent`. The stateful overload SHALL pass `viewModel.uiEffect` through to the stateless composable.

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

#### Scenario: A toggle-failure effect shows a snackbar with the error message
- **GIVEN** `ProductListScreen(uiState, onIntent, uiEffect)` is composed with a `uiEffect` flow that emits `ProductListUiEffect.ShowFavouriteToggleError`
- **WHEN** that emission is collected
- **THEN** a `Snackbar` displaying the localised `favourite_toggle_error_message` text is shown

#### Scenario: Tapping a card's favourite icon dispatches ToggleFavourite
- **GIVEN** `uiState` is `ProductListUiState.Content` containing a product with `id = 7`
- **WHEN** the user taps that product card's favourite icon
- **THEN** `ProductListUiIntent.ToggleFavourite(productId = 7)` is dispatched to `onIntent`

### Requirement: ProductListItemCard displays image, title, price, rating score, and a favourite toggle icon
`:app` SHALL define a stateless `ProductListItemCard(item: ProductListItem, onToggleFavourite: (Int) -> Unit = {}, modifier: Modifier = Modifier)` composable displaying, for a single `ProductListItem`: an asynchronously loaded product image with a content description derived from the product title, the full product title (wrapping to multiple lines rather than being clipped), the pre-formatted price string, the pre-formatted rating score string, and a heart `IconButton` whose icon is filled when `item.isFavourite` is `true` and outlined when `false`, whose content description is resolved via `stringResource(if (item.isFavourite) R.string.favourite_remove_content_description else R.string.favourite_add_content_description)`, and which invokes `onToggleFavourite(item.id)` when tapped. The composable SHALL perform no formatting, locale detection, or other business logic — it renders only the values already present on `ProductListItem`.

#### Scenario: Card displays all required fields for a given item
- **GIVEN** a `ProductListItem` with a title, `formattedPrice`, and `formattedRatingScore`
- **WHEN** `ProductListItemCard(item)` is composed
- **THEN** the title text, the formatted price text, and the formatted rating score text are each present and visible in the composed output

#### Scenario: Long product titles wrap instead of being clipped
- **GIVEN** a `ProductListItem` whose `title` is long enough to exceed one line at the card's width
- **WHEN** `ProductListItemCard(item)` is composed
- **THEN** the title text wraps to multiple lines and is not truncated or overlapping other card content

#### Scenario: The filled heart icon is shown for a favourited item
- **GIVEN** a `ProductListItem` with `isFavourite = true`
- **WHEN** `ProductListItemCard(item)` is composed
- **THEN** the filled favourite icon is displayed with the localised "Remove from favourites" content description

#### Scenario: The outlined heart icon is shown for a non-favourited item
- **GIVEN** a `ProductListItem` with `isFavourite = false`
- **WHEN** `ProductListItemCard(item)` is composed
- **THEN** the outlined favourite icon is displayed with the localised "Add to favourites" content description

#### Scenario: Tapping the favourite icon invokes onToggleFavourite with the item's ID
- **GIVEN** a `ProductListItem` with `id = 7`
- **WHEN** the user taps the favourite icon on `ProductListItemCard(item, onToggleFavourite)`
- **THEN** `onToggleFavourite` is invoked with `7`

### Requirement: Product images load asynchronously with a placeholder and failure fallback
`ProductListItemCard` SHALL load `ProductListItem.imageUrl` asynchronously via Coil 3's `AsyncImage`, displaying a Material icon placeholder painter while the image is loading, and the same or a similar Material icon painter as the `error` fallback if the image fails to load.

#### Scenario: Placeholder is visible before the image finishes loading
- **GIVEN** `ProductListItemCard` is composed for an item whose image has not yet finished loading
- **WHEN** the composition is inspected before the image request completes
- **THEN** a Material icon placeholder is displayed in place of the product image

#### Scenario: Fallback icon is shown when the image fails to load
- **GIVEN** `ProductListItemCard` is composed for an item whose `imageUrl` resolves to a failed image request
- **WHEN** the image request completes with an error
- **THEN** a Material icon fallback is displayed instead of a broken or blank image area

### Requirement: ProductListScreen is the app's initial screen
`MainActivity` SHALL render the stateful `ProductListScreen()` composable as the sole content of its `Scaffold`, replacing the previous `Greeting("Android")` placeholder, so the product list is the first screen shown on app launch.

#### Scenario: App launch shows the product list screen
- **GIVEN** the app is launched fresh
- **WHEN** `MainActivity.onCreate()` completes and initial composition finishes
- **THEN** `ProductListScreen` is composed inside `MainActivity`'s `Scaffold`, and no `Greeting` composable is present anywhere in the composition

### Requirement: User-facing strings are localised in English and Spanish
Every user-facing string introduced by this screen (e.g. the screen title, the product image content description) SHALL be defined as a string resource in `app/src/main/res/values/strings.xml` (English, base) with a corresponding translated entry in `app/src/main/res/values-es/strings.xml` (generic Spanish), and SHALL NOT appear as a hardcoded literal in any composable.

#### Scenario: Every new string resource has a Spanish translation
- **WHEN** `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml` are compared
- **THEN** every string key added for the product list screen exists in both files with a non-empty, distinct Spanish value in `values-es`

#### Scenario: No hardcoded user-facing string literals in composables
- **WHEN** `ProductListScreen.kt` and `ProductListItemCard.kt` are inspected
- **THEN** all user-facing text is sourced via `stringResource(R.string.*)`, with no inline string literals passed to `Text` or `contentDescription`

### Requirement: Error, retry, and empty-state strings are localised in English and Spanish
`app/src/main/res/values/strings.xml` SHALL define `product_list_error_message`, `product_list_retry_button`, and `product_list_empty_message` as English (base) string resources, each with a corresponding translated entry in `app/src/main/res/values-es/strings.xml`, and none of the three SHALL appear as a hardcoded literal in `ProductListScreen.kt`.

#### Scenario: Every new string resource has a Spanish translation
- **WHEN** `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml` are compared
- **THEN** `product_list_error_message`, `product_list_retry_button`, and `product_list_empty_message` each exist in both files with a non-empty, distinct Spanish value in `values-es`

#### Scenario: No hardcoded literals for the new user-facing strings
- **WHEN** `ProductListScreen.kt` is inspected
- **THEN** the error message, retry button label, and empty-state message are each sourced via `stringResource(R.string.*)`, with no inline string literals

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
