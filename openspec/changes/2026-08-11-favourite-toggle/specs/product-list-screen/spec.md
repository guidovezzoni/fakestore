## ADDED Requirements

### Requirement: ProductListItem carries pre-computed favourite state
`:app` SHALL extend `ProductListItem` with `isFavourite: Boolean` (default `false`) and `favouriteContentDescription: String` (default `""`), both computed by `ProductListViewModel`/`FavouritesViewModel` when mapping a domain `Product` — never derived, formatted, or looked up inside a composable.

#### Scenario: A favourited product's item reflects isFavourite = true
- **GIVEN** a `Product` whose `id` is present in the current favourite ID set
- **WHEN** it is mapped to a `ProductListItem`
- **THEN** `isFavourite` equals `true`

#### Scenario: An unfavourited product's item reflects isFavourite = false
- **GIVEN** a `Product` whose `id` is absent from the current favourite ID set
- **WHEN** it is mapped to a `ProductListItem`
- **THEN** `isFavourite` equals `false`

### Requirement: ProductListUiEffect models a favourite toggle failure notification
`:app` SHALL define `ProductListUiEffect` as a sealed interface with `ShowFavouriteToggleError`, emitted by `ProductListViewModel` when a `ToggleFavourite` intent's underlying write fails, and consumed exactly once by `ProductListScreen` to show a snackbar.

#### Scenario: ShowFavouriteToggleError is emitted on a failed toggle
- **GIVEN** a `ProductListViewModel` backed by a mocked `ToggleFavouriteUseCase` that returns `Result.failure(...)`
- **WHEN** `ProductListUiIntent.ToggleFavourite(productId = 7)` is dispatched
- **THEN** `uiEffect` emits `ProductListUiEffect.ShowFavouriteToggleError`

## MODIFIED Requirements

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

### Requirement: ProductListViewModel combines products with reactive favourite state and handles optimistic toggling
`ProductListViewModel` SHALL additionally be constructor-injected with `GetFavouriteIdsUseCase`, `ToggleFavouriteUseCase`, and `Context` (via `@ApplicationContext`). On a successful product fetch, it SHALL `combine()` the fetched products with `GetFavouriteIdsUseCase()`'s reactive `Flow<Set<Int>>`, mapping each product to a `ProductListItem` with `isFavourite` set from set membership and `favouriteContentDescription` resolved accordingly, and SHALL re-derive `ProductListUiState.Content` whenever the favourite ID set changes, without requiring a new `LoadProducts`/`RetryClicked` dispatch.

On `ProductListUiIntent.ToggleFavourite(productId)`, it SHALL immediately flip that item's `isFavourite` (and recompute its `favouriteContentDescription`) in the currently displayed `Content.products` (optimistic update), then call `ToggleFavouriteUseCase(productId, shouldBeFavourite = <the new state>)`. On success, it SHALL log `favourite_added` or `favourite_removed` (selected by the new state) with `params = mapOf("product_id" to productId)`. On failure, it SHALL restore the pre-toggle `products` list and emit `ProductListUiEffect.ShowFavouriteToggleError`; it SHALL NOT log any analytics event in this case.

The existing Loading/Content/Error emission behaviour on `LoadProducts`/`RetryClicked` (including empty-list handling and `product_list_viewed` logging) is unchanged by this requirement.

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

### Requirement: ProductListItemCard displays image, title, price, rating score, and a favourite toggle icon
`:app` SHALL define a stateless `ProductListItemCard(item: ProductListItem, onToggleFavourite: (Int) -> Unit = {}, modifier: Modifier = Modifier)` composable displaying, for a single `ProductListItem`: an asynchronously loaded product image, the full product title, the pre-formatted price and rating score strings (unchanged from the existing requirement), and a heart `IconButton` whose icon is filled when `item.isFavourite` is `true` and outlined when `false`, whose content description is `item.favouriteContentDescription`, and which invokes `onToggleFavourite(item.id)` when tapped. The composable SHALL perform no formatting, locale detection, or other business logic — it renders only values already present on `ProductListItem`.

#### Scenario: The filled heart icon is shown for a favourited item
- **GIVEN** a `ProductListItem` with `isFavourite = true`
- **WHEN** `ProductListItemCard(item)` is composed
- **THEN** the filled favourite icon is displayed with content description equal to `item.favouriteContentDescription`

#### Scenario: The outlined heart icon is shown for a non-favourited item
- **GIVEN** a `ProductListItem` with `isFavourite = false`
- **WHEN** `ProductListItemCard(item)` is composed
- **THEN** the outlined favourite icon is displayed with content description equal to `item.favouriteContentDescription`

#### Scenario: Tapping the favourite icon invokes onToggleFavourite with the item's ID
- **GIVEN** a `ProductListItem` with `id = 7`
- **WHEN** the user taps the favourite icon on `ProductListItemCard(item, onToggleFavourite)`
- **THEN** `onToggleFavourite` is invoked with `7`

### Requirement: ProductListScreen shows a snackbar when a favourite toggle fails
`:app`'s stateless `ProductListScreen` composable SHALL additionally accept `uiEffect: Flow<ProductListUiEffect> = emptyFlow()`, collect it, and on `ProductListUiEffect.ShowFavouriteToggleError`, show a `Snackbar` (via a `Scaffold`'s `snackbarHost`) displaying the localised `favourite_toggle_error_message`. Each rendered `ProductListItemCard` SHALL be wired so that tapping its favourite icon dispatches `ProductListUiIntent.ToggleFavourite(item.id)` to `onIntent`. The stateful overload SHALL pass `viewModel.uiEffect` through to the stateless composable.

#### Scenario: A toggle-failure effect shows a snackbar with the error message
- **GIVEN** `ProductListScreen(uiState, onIntent, uiEffect)` is composed with a `uiEffect` flow that emits `ProductListUiEffect.ShowFavouriteToggleError`
- **WHEN** that emission is collected
- **THEN** a `Snackbar` displaying the localised `favourite_toggle_error_message` text is shown

#### Scenario: Tapping a card's favourite icon dispatches ToggleFavourite
- **GIVEN** `uiState` is `ProductListUiState.Content` containing a product with `id = 7`
- **WHEN** the user taps that product card's favourite icon
- **THEN** `ProductListUiIntent.ToggleFavourite(productId = 7)` is dispatched to `onIntent`
