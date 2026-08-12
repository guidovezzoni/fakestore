## ADDED Requirements

### Requirement: favourites_screen_viewed is logged once Content is available
On `FavouritesUiIntent.TrackScreenViewed`, `FavouritesViewModel` SHALL launch a coroutine that waits for `uiState` to reach `FavouritesUiState.Content`, then logs `favourites_screen_viewed` via `AnalyticsClient.logEvent()` with `params = mapOf("favourite_count" to <the Content's products.size at that emission>)`. If `uiState` is `FavouritesUiState.Loading` or `FavouritesUiState.Error` at dispatch time, no event SHALL be logged until (and unless) `Content` is subsequently reached; no event SHALL be logged at all while `uiState` remains `Error`. Each `TrackScreenViewed` dispatch SHALL be tracked independently, so dispatching it multiple times SHALL log the event multiple times.

#### Scenario: TrackScreenViewed dispatched against existing Content logs immediately with the current count
- **GIVEN** `uiState.value` is `FavouritesUiState.Content` containing 3 favourite products
- **WHEN** `FavouritesUiIntent.TrackScreenViewed` is dispatched
- **THEN** `AnalyticsClient.logEvent()` is invoked exactly once with `name = "favourites_screen_viewed"` and `params = mapOf("favourite_count" to 3)`

#### Scenario: TrackScreenViewed dispatched against empty Content logs with a zero count
- **GIVEN** `uiState.value` is `FavouritesUiState.Content(products = emptyList())`
- **WHEN** `FavouritesUiIntent.TrackScreenViewed` is dispatched
- **THEN** `AnalyticsClient.logEvent()` is invoked exactly once with `name = "favourites_screen_viewed"` and `params = mapOf("favourite_count" to 0)`

#### Scenario: TrackScreenViewed dispatched against Loading does not log until Content is reached
- **GIVEN** `uiState.value` is `FavouritesUiState.Loading`
- **WHEN** `FavouritesUiIntent.TrackScreenViewed` is dispatched and no `Content` emission has occurred yet
- **THEN** `AnalyticsClient.logEvent()` is never invoked with `name = "favourites_screen_viewed"`

#### Scenario: TrackScreenViewed dispatched against Error never logs
- **GIVEN** `uiState.value` is `FavouritesUiState.Error`
- **WHEN** `FavouritesUiIntent.TrackScreenViewed` is dispatched and `uiState` remains `Error`
- **THEN** `AnalyticsClient.logEvent()` is never invoked with `name = "favourites_screen_viewed"`

#### Scenario: Dispatching TrackScreenViewed twice against Content logs twice
- **GIVEN** `uiState.value` is `FavouritesUiState.Content` containing favourite products
- **WHEN** `FavouritesUiIntent.TrackScreenViewed` is dispatched twice
- **THEN** `AnalyticsClient.logEvent()` is invoked exactly twice with `name = "favourites_screen_viewed"`

## MODIFIED Requirements

### Requirement: FavouritesUiIntent models loading, toggling a favourite, and tracking a screen view from the Favourites screen
`:app` SHALL define `FavouritesUiIntent` as a sealed interface with `LoadFavourites` (triggered on first composition), `ToggleFavourite(val productId: Int)` (triggered by tapping a card's heart icon), and `TrackScreenViewed` (triggered on first composition, signalling `FavouritesViewModel` to log the `favourites_screen_viewed` analytics event once data is available).

#### Scenario: LoadFavourites, ToggleFavourite, and TrackScreenViewed are distinct sealed entries
- **WHEN** `FavouritesUiIntent.LoadFavourites`, `FavouritesUiIntent.ToggleFavourite(productId = 7)`, and `FavouritesUiIntent.TrackScreenViewed` are referenced
- **THEN** all three are members of the `FavouritesUiIntent` sealed interface, usable in an exhaustive `when` block over `FavouritesViewModel.onIntent()`

### Requirement: FavouritesScreen renders state-specific UI with an empty-state message and a snackbar on toggle failure
`:app` SHALL define a stateless `FavouritesScreen(uiState: FavouritesUiState, onIntent: (FavouritesUiIntent) -> Unit, uiEffect: Flow<FavouritesUiEffect> = emptyFlow(), modifier: Modifier = Modifier)` composable. It SHALL render a centred loading indicator for `Loading`; for `Content`, either a `LazyColumn` of `ProductListItemCard`s (keyed by `id`, each wired to dispatch `ToggleFavourite`) when `products` is non-empty, or a distinct empty-state message (`favourites_empty_message`) when empty; and a centred, user-facing error message for `Error`. It SHALL collect `uiEffect` and show a `Snackbar` with the localised `favourite_toggle_error_message` on `ShowFavouriteToggleError`. A stateful overload SHALL obtain `FavouritesViewModel` via `hiltViewModel()`, dispatch both `LoadFavourites` and `TrackScreenViewed` from the same `LaunchedEffect(Unit)` on first composition, and pass `viewModel.uiEffect` through.

#### Scenario: Loading state shows only the loading indicator
- **GIVEN** `uiState` is `FavouritesUiState.Loading`
- **WHEN** `FavouritesScreen(uiState, onIntent)` is composed
- **THEN** a loading indicator is displayed and no product cards or empty-state message are visible

#### Scenario: Content with favourited products shows the product list
- **GIVEN** `uiState` is `FavouritesUiState.Content` with a non-empty `products` list
- **WHEN** `FavouritesScreen(uiState, onIntent)` is composed
- **THEN** a `LazyColumn` is displayed containing one card per item in `products`, each keyed by the item's `id`

#### Scenario: Content with no favourites shows the empty-state message
- **GIVEN** `uiState` is `FavouritesUiState.Content(products = emptyList())`
- **WHEN** `FavouritesScreen(uiState, onIntent)` is composed
- **THEN** the localised `favourites_empty_message` text is displayed and no product cards are visible

#### Scenario: Error state shows a centred error message
- **GIVEN** `uiState` is `FavouritesUiState.Error`
- **WHEN** `FavouritesScreen(uiState, onIntent)` is composed
- **THEN** a centred error message is displayed and no product cards, loading indicator, or empty-state message are visible

#### Scenario: Removing a favourite from the Favourites screen removes it from the displayed list
- **GIVEN** `uiState` is `FavouritesUiState.Content` with a product `id = 7` displayed
- **WHEN** the user taps that product card's heart icon
- **THEN** `FavouritesUiIntent.ToggleFavourite(productId = 7)` is dispatched to `onIntent`

#### Scenario: LoadFavourites and TrackScreenViewed both fire on first composition
- **GIVEN** the stateful `FavouritesScreen()` overload is composed for the first time
- **WHEN** composition completes
- **THEN** `FavouritesUiIntent.LoadFavourites` and `FavouritesUiIntent.TrackScreenViewed` have each been dispatched to the underlying `FavouritesViewModel.onIntent()` exactly once

#### Scenario: A toggle-failure effect shows a snackbar with the error message
- **GIVEN** `FavouritesScreen(uiState, onIntent, uiEffect)` is composed with a `uiEffect` flow that emits `FavouritesUiEffect.ShowFavouriteToggleError`
- **WHEN** that emission is collected
- **THEN** a `Snackbar` displaying the localised `favourite_toggle_error_message` text is shown

### Requirement: Favourite-related strings are localised in English and Spanish
`app/src/main/res/values/strings.xml` SHALL define `favourite_add_content_description`, `favourite_remove_content_description`, `favourite_toggle_error_message`, and `favourites_empty_message` as English (base) string resources, each with a corresponding translated entry in `app/src/main/res/values-es/strings.xml`, and none SHALL appear as a hardcoded literal in source code. Content description strings are resolved in composables via `stringResource()` based on `isFavourite`, not pre-computed in ViewModels. `favourites_empty_message` SHALL read "No favourites yet. Tap the heart on a product to save it." in `values/strings.xml`, giving first-time visitors actionable guidance rather than only stating absence.

#### Scenario: Every new string resource has a Spanish translation
- **WHEN** `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml` are compared
- **THEN** `favourite_add_content_description`, `favourite_remove_content_description`, `favourite_toggle_error_message`, and `favourites_empty_message` each exist in both files with a non-empty, distinct Spanish value in `values-es`

#### Scenario: The empty-state message includes instructional guidance
- **WHEN** `FavouritesUiState.Content(products = emptyList())` is rendered
- **THEN** the displayed text is exactly "No favourites yet. Tap the heart on a product to save it." (English) or its Spanish equivalent in `values-es`, not merely "No favourites yet"
