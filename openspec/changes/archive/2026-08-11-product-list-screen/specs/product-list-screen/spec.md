## ADDED Requirements

### Requirement: ProductListUiState exposes a display-ready product list
`:app` SHALL define `ProductListUiState` as an immutable data class with a single field `products: List<ProductListItem>` defaulting to `emptyList()`, where `ProductListItem` is a display-ready data class (`id: Int`, `imageUrl: String`, `title: String`, `formattedPrice: String`, `formattedRatingScore: String`) containing no unformatted domain values.

#### Scenario: Default UiState has an empty product list
- **WHEN** `ProductListUiState()` is constructed with no arguments
- **THEN** `products` equals `emptyList()`

### Requirement: ProductListUiIntent models the initial load action
`:app` SHALL define `ProductListUiIntent` as a sealed class with a `LoadProducts` entry, representing the intent triggered on the screen's first composition.

#### Scenario: LoadProducts is a distinct sealed subtype
- **WHEN** `ProductListUiIntent.LoadProducts` is referenced
- **THEN** it is a member of the `ProductListUiIntent` sealed class, usable in an exhaustive `when` block over `ProductListViewModel.onIntent()`

### Requirement: ProductListUiEffect establishes the one-shot effect structure
`:app` SHALL define `ProductListUiEffect` as a sealed class with no entries in this story, establishing the type used by `ProductListViewModel.uiEffect: SharedFlow<ProductListUiEffect>` for future one-shot effects.

#### Scenario: UiEffect type exists and is exposed by the ViewModel
- **WHEN** `ProductListViewModel.uiEffect` is inspected
- **THEN** its type is `SharedFlow<ProductListUiEffect>`, even though no `ProductListUiEffect` instance is emitted by this story's behaviour

### Requirement: ProductListViewModel loads and maps products on LoadProducts
`ProductListViewModel` SHALL be annotated `@HiltViewModel`, constructor-injected with `GetProductsUseCase`, and expose `uiState: StateFlow<ProductListUiState>` and `uiEffect: SharedFlow<ProductListUiEffect>`. On receiving `ProductListUiIntent.LoadProducts` via `onIntent()`, it SHALL invoke `GetProductsUseCase()`, and on a `Result.success` emission, map each domain `Product` to a `ProductListItem` and update `uiState.products` with the resulting list, preserving the source order.

#### Scenario: LoadProducts populates uiState with mapped products
- **GIVEN** a mocked `GetProductsUseCase` whose `invoke()` returns a `Flow` emitting `Result.success` with a list of `Product` domain models
- **WHEN** `ProductListViewModel.onIntent(ProductListUiIntent.LoadProducts)` is called and collection completes
- **THEN** `uiState.value.products` contains one `ProductListItem` per input `Product`, in the same order, with `id` equal to the source `Product.id`

#### Scenario: Repeated LoadProducts intents do not corrupt state
- **GIVEN** a mocked `GetProductsUseCase` returning a fixed successful product list
- **WHEN** `ProductListUiIntent.LoadProducts` is dispatched twice via `onIntent()`
- **THEN** `uiState.value.products` still equals the mapped list from the (idempotent) use case result, with no duplicated entries

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

### Requirement: ProductListScreen renders a scrollable list of product cards
`:app` SHALL define a stateless `ProductListScreen(uiState: ProductListUiState, onIntent: (ProductListUiIntent) -> Unit, modifier: Modifier = Modifier)` composable rendering `uiState.products` inside a `LazyColumn` using `key = { it.id }`, with one `ProductListItemCard` per item, and a stateful overload obtaining `ProductListViewModel` via `hiltViewModel()` and dispatching `ProductListUiIntent.LoadProducts` on first composition.

#### Scenario: Products are rendered as a scrollable list
- **GIVEN** a `ProductListUiState` with a non-empty `products` list
- **WHEN** `ProductListScreen(uiState, onIntent)` is composed
- **THEN** a `LazyColumn` is displayed containing one card per item in `uiState.products`, each keyed by the item's `id`

#### Scenario: LoadProducts intent fires on first composition
- **GIVEN** the stateful `ProductListScreen()` overload is composed for the first time
- **WHEN** composition completes
- **THEN** `ProductListUiIntent.LoadProducts` has been dispatched to the underlying `ProductListViewModel.onIntent()` exactly once

### Requirement: ProductListItemCard displays image, title, price, and rating score
`:app` SHALL define a stateless `ProductListItemCard(item: ProductListItem, modifier: Modifier = Modifier)` composable displaying, for a single `ProductListItem`: an asynchronously loaded product image with a content description derived from the product title, the full product title (wrapping to multiple lines rather than being clipped), the pre-formatted price string, and the pre-formatted rating score string. The composable SHALL perform no formatting, locale detection, or other business logic — it renders only the values already present on `ProductListItem`.

#### Scenario: Card displays all required fields for a given item
- **GIVEN** a `ProductListItem` with a title, `formattedPrice`, and `formattedRatingScore`
- **WHEN** `ProductListItemCard(item)` is composed
- **THEN** the title text, the formatted price text, and the formatted rating score text are each present and visible in the composed output

#### Scenario: Long product titles wrap instead of being clipped
- **GIVEN** a `ProductListItem` whose `title` is long enough to exceed one line at the card's width
- **WHEN** `ProductListItemCard(item)` is composed
- **THEN** the title text wraps to multiple lines and is not truncated or overlapping other card content

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
