## ADDED Requirements

### Requirement: Favourite product IDs are persisted in a local Room table
`:data` SHALL define a Room `FavouriteEntity` (table `favourite_entity`) with a single field `productId: Int` annotated `@PrimaryKey`, and no other columns. `:data` SHALL define a `FavouriteDao` with `suspend fun insert(entity: FavouriteEntity)` (via `@Insert` with conflict resolution that replaces an existing row rather than throwing), `suspend fun delete(entity: FavouriteEntity)` (via `@Delete`), and `fun getAllIds(): Flow<List<Int>>` (via `@Query`) returning every persisted `productId` as a reactive stream. `:data` SHALL define a Room `FavouritesDatabase` (`@Database(entities = [FavouriteEntity::class], version = 1)`) exposing `favouriteDao(): FavouriteDao`.

#### Scenario: Adding a favourite inserts a row
- **GIVEN** an empty `favourite_entity` table
- **WHEN** `FavouriteDao.insert(FavouriteEntity(productId = 7))` is called
- **THEN** `getAllIds()` emits a list containing `7`

#### Scenario: Removing a favourite deletes its row
- **GIVEN** `favourite_entity` contains a row with `productId = 7`
- **WHEN** `FavouriteDao.delete(FavouriteEntity(productId = 7))` is called
- **THEN** `getAllIds()` emits a list no longer containing `7`

#### Scenario: Only the product ID is persisted
- **WHEN** `FavouriteEntity`'s fields are inspected
- **THEN** `productId: Int` is the only field — no title, price, image, or other product data is present on the entity

### Requirement: FavouritesRepository defines the domain contract for favourite operations
`:domain` SHALL define a `FavouritesRepository` interface with `suspend fun addFavourite(productId: Int)`, `suspend fun removeFavourite(productId: Int)`, and `fun getFavouriteIds(): Flow<Set<Int>>`. `:data` SHALL provide `FavouritesRepositoryImpl`, which delegates `addFavourite`/`removeFavourite` to `FavouriteDao.insert`/`delete`, and converts `FavouriteDao.getAllIds()`'s `Flow<List<Int>>` into `Flow<Set<Int>>` for `getFavouriteIds()`. `:domain` SHALL have no dependency on Room, Android, or any `:data`-layer type.

#### Scenario: addFavourite delegates to the DAO
- **GIVEN** a `FavouritesRepositoryImpl` backed by a mocked `FavouriteDao`
- **WHEN** `addFavourite(productId = 7)` is called
- **THEN** `FavouriteDao.insert(FavouriteEntity(productId = 7))` is invoked exactly once

#### Scenario: removeFavourite delegates to the DAO
- **GIVEN** a `FavouritesRepositoryImpl` backed by a mocked `FavouriteDao`
- **WHEN** `removeFavourite(productId = 7)` is called
- **THEN** `FavouriteDao.delete(FavouriteEntity(productId = 7))` is invoked exactly once

#### Scenario: getFavouriteIds converts the DAO's list flow into a set flow
- **GIVEN** a `FavouritesRepositoryImpl` backed by a mocked `FavouriteDao` whose `getAllIds()` emits `listOf(3, 7, 7)`
- **WHEN** `getFavouriteIds()` is collected
- **THEN** it emits `setOf(3, 7)`

### Requirement: ToggleFavouriteUseCase adds or removes a favourite based on the caller's target state
`:domain` SHALL define `ToggleFavouriteUseCase(private val repository: FavouritesRepository)` with `suspend operator fun invoke(productId: Int, shouldBeFavourite: Boolean): Result<Unit>`. When `shouldBeFavourite` is `true`, it SHALL call `repository.addFavourite(productId)`; when `false`, `repository.removeFavourite(productId)`. Any exception thrown by the repository call SHALL be caught and returned as `Result.failure`, never thrown to the caller.

#### Scenario: Toggling to favourited calls addFavourite
- **GIVEN** a `ToggleFavouriteUseCase` backed by a mocked `FavouritesRepository`
- **WHEN** `invoke(productId = 7, shouldBeFavourite = true)` is called
- **THEN** `FavouritesRepository.addFavourite(7)` is invoked exactly once and the use case returns `Result.success(Unit)`

#### Scenario: Toggling to unfavourited calls removeFavourite
- **GIVEN** a `ToggleFavouriteUseCase` backed by a mocked `FavouritesRepository`
- **WHEN** `invoke(productId = 7, shouldBeFavourite = false)` is called
- **THEN** `FavouritesRepository.removeFavourite(7)` is invoked exactly once and the use case returns `Result.success(Unit)`

#### Scenario: A repository failure is returned as Result.failure, not thrown
- **GIVEN** a `ToggleFavouriteUseCase` backed by a mocked `FavouritesRepository` whose `addFavourite` throws an exception
- **WHEN** `invoke(productId = 7, shouldBeFavourite = true)` is called
- **THEN** the call returns `Result.failure` wrapping that exception, and no exception escapes the `invoke()` call

### Requirement: GetFavouriteIdsUseCase exposes the reactive favourite ID set
`:domain` SHALL define `GetFavouriteIdsUseCase(private val repository: FavouritesRepository)` with `operator fun invoke(): Flow<Set<Int>>`, delegating directly to `repository.getFavouriteIds()`.

#### Scenario: Use case returns the repository's flow of favourite IDs
- **GIVEN** a `GetFavouriteIdsUseCase` backed by a mocked `FavouritesRepository` whose `getFavouriteIds()` emits `setOf(1, 2)`
- **WHEN** `invoke()` is collected
- **THEN** it emits `setOf(1, 2)`

#### Scenario: An empty favourites table yields an empty set
- **GIVEN** a `GetFavouriteIdsUseCase` backed by a mocked `FavouritesRepository` whose `getFavouriteIds()` emits `emptySet()`
- **WHEN** `invoke()` is collected
- **THEN** it emits `emptySet()`

### Requirement: favourite_added and favourite_removed are logged only after a successful persistence write
On a successful `ToggleFavouriteUseCase` call, the calling ViewModel SHALL log `favourite_added` (when `shouldBeFavourite` was `true`) or `favourite_removed` (when `false`) via `AnalyticsClient.logEvent()`, with `params = mapOf("product_id" to productId)`. No analytics event SHALL be logged when the `ToggleFavouriteUseCase` call returns `Result.failure`.

#### Scenario: A successful add logs favourite_added with the product ID
- **GIVEN** a ViewModel constructed with a mocked `ToggleFavouriteUseCase` that returns `Result.success(Unit)` and a mocked `AnalyticsClient`
- **WHEN** a toggle-to-favourite intent for `productId = 7` is dispatched and the write succeeds
- **THEN** `AnalyticsClient.logEvent()` is invoked exactly once with `name = "favourite_added"` and `params = mapOf("product_id" to 7)`

#### Scenario: A successful removal logs favourite_removed with the product ID
- **GIVEN** a ViewModel constructed with a mocked `ToggleFavouriteUseCase` that returns `Result.success(Unit)` and a mocked `AnalyticsClient`
- **WHEN** a toggle-to-unfavourite intent for `productId = 7` is dispatched and the write succeeds
- **THEN** `AnalyticsClient.logEvent()` is invoked exactly once with `name = "favourite_removed"` and `params = mapOf("product_id" to 7)`

#### Scenario: A failed toggle logs no analytics event
- **GIVEN** a ViewModel constructed with a mocked `ToggleFavouriteUseCase` that returns `Result.failure(...)` and a mocked `AnalyticsClient`
- **WHEN** a toggle intent is dispatched and the write fails
- **THEN** `AnalyticsClient.logEvent()` is never invoked with `name = "favourite_added"` or `name = "favourite_removed"`

### Requirement: FavouritesUiState mirrors ProductListUiState's Loading/Content/Error shape
`:app` SHALL define `FavouritesUiState` as a `sealed interface` with `Loading` (a `data object`), `Content(val products: List<ProductListItem>)` (a `data class`), and `Error` (a `data object`), matching `ProductListUiState`'s existing three-variant contract.

#### Scenario: Default entry point is Loading
- **GIVEN** a newly constructed `FavouritesViewModel`
- **WHEN** no intent has been dispatched
- **THEN** `uiState.value` is `FavouritesUiState.Loading`

### Requirement: FavouritesUiIntent models loading and toggling a favourite from the Favourites screen
`:app` SHALL define `FavouritesUiIntent` as a sealed interface with `LoadFavourites` (triggered on first composition) and `ToggleFavourite(val productId: Int)` (triggered by tapping a card's heart icon).

#### Scenario: LoadFavourites and ToggleFavourite are distinct sealed entries
- **WHEN** `FavouritesUiIntent.LoadFavourites` and `FavouritesUiIntent.ToggleFavourite(productId = 7)` are referenced
- **THEN** both are members of the `FavouritesUiIntent` sealed interface, usable in an exhaustive `when` block over `FavouritesViewModel.onIntent()`

### Requirement: FavouritesUiEffect models a favourite toggle failure notification
`:app` SHALL define `FavouritesUiEffect` as a sealed interface with `ShowFavouriteToggleError`, emitted by `FavouritesViewModel` when a `ToggleFavourite` intent's underlying write fails.

#### Scenario: ShowFavouriteToggleError is emitted on a failed toggle
- **GIVEN** a `FavouritesViewModel` backed by a mocked `ToggleFavouriteUseCase` that returns `Result.failure(...)`
- **WHEN** `FavouritesUiIntent.ToggleFavourite(productId = 7)` is dispatched
- **THEN** `uiEffect` emits `FavouritesUiEffect.ShowFavouriteToggleError`

### Requirement: FavouritesViewModel shows only favourited products, kept in sync with Room
`FavouritesViewModel` SHALL be annotated `@HiltViewModel`, constructor-injected with `GetProductsUseCase`, `GetFavouriteIdsUseCase`, `ToggleFavouriteUseCase`, and `AnalyticsClient` (no `Context` dependency). On `LoadFavourites`, it SHALL invoke `GetProductsUseCase()`; on success, it SHALL combine the fetched products with `GetFavouriteIdsUseCase()`'s reactive `Flow<Set<Int>>`, filter to only products whose `id` is present in the favourite ID set, map each to a `ProductListItem` with `isFavourite = true`, and set `uiState` to `FavouritesUiState.Content` with the filtered, mapped list — including when the filtered list is empty. On failure, it SHALL set `uiState` to `FavouritesUiState.Error`.

#### Scenario: Only favourited products appear in Content
- **GIVEN** a mocked `GetProductsUseCase` returning three products with ids `1`, `2`, `3`, and a mocked `GetFavouriteIdsUseCase` emitting `setOf(2)`
- **WHEN** `FavouritesUiIntent.LoadFavourites` is dispatched
- **THEN** `uiState.value` is `FavouritesUiState.Content` containing exactly one `ProductListItem` with `id = 2` and `isFavourite = true`

#### Scenario: No favourites yields an empty Content list
- **GIVEN** a mocked `GetProductsUseCase` returning a non-empty product list, and a mocked `GetFavouriteIdsUseCase` emitting `emptySet()`
- **WHEN** `FavouritesUiIntent.LoadFavourites` is dispatched
- **THEN** `uiState.value` equals `FavouritesUiState.Content(products = emptyList())`

#### Scenario: A change to the favourites table updates Content without a new LoadFavourites dispatch
- **GIVEN** `FavouritesUiIntent.LoadFavourites` has already been dispatched and `uiState.value` is `FavouritesUiState.Content` containing a product with `id = 2`
- **WHEN** the mocked `GetFavouriteIdsUseCase`'s underlying flow emits a new set no longer containing `2` (simulating a removal made from the Products screen)
- **THEN** `uiState.value` updates to `FavouritesUiState.Content` no longer containing that product, with no new intent dispatched

### Requirement: Toggling a favourite from the Favourites screen optimistically removes it, with revert on failure
On `FavouritesUiIntent.ToggleFavourite(productId)`, `FavouritesViewModel` SHALL immediately remove the item with that `id` from the displayed `FavouritesUiState.Content.products` (optimistic update), then call `ToggleFavouriteUseCase(productId, shouldBeFavourite = false)`. On success, it SHALL log `favourite_removed` with `params = mapOf("product_id" to productId)`. On failure, it SHALL restore the pre-toggle `products` list and emit `FavouritesUiEffect.ShowFavouriteToggleError`.

#### Scenario: Toggling immediately removes the item from the displayed list
- **GIVEN** `uiState.value` is `FavouritesUiState.Content` containing a product with `id = 7`
- **WHEN** `FavouritesUiIntent.ToggleFavourite(productId = 7)` is dispatched, before the underlying write completes
- **THEN** `uiState.value`'s `products` no longer contains a product with `id = 7`

#### Scenario: A failed removal restores the item and shows an error
- **GIVEN** `uiState.value` is `FavouritesUiState.Content` containing a product with `id = 7`, and a mocked `ToggleFavouriteUseCase` that returns `Result.failure(...)`
- **WHEN** `FavouritesUiIntent.ToggleFavourite(productId = 7)` is dispatched and the write completes
- **THEN** `uiState.value`'s `products` once again contains the product with `id = 7`, and `uiEffect` emits `FavouritesUiEffect.ShowFavouriteToggleError`

### Requirement: FavouritesScreen renders state-specific UI with an empty-state message and a snackbar on toggle failure
`:app` SHALL define a stateless `FavouritesScreen(uiState: FavouritesUiState, onIntent: (FavouritesUiIntent) -> Unit, uiEffect: Flow<FavouritesUiEffect> = emptyFlow(), modifier: Modifier = Modifier)` composable. It SHALL render a centred loading indicator for `Loading`; for `Content`, either a `LazyColumn` of `ProductListItemCard`s (keyed by `id`, each wired to dispatch `ToggleFavourite`) when `products` is non-empty, or a distinct empty-state message (`favourites_empty_message`) when empty; and a user-facing error message for `Error`. It SHALL collect `uiEffect` and show a `Snackbar` with the localised `favourite_toggle_error_message` on `ShowFavouriteToggleError`. A stateful overload SHALL obtain `FavouritesViewModel` via `hiltViewModel()`, dispatch `LoadFavourites` on first composition, and pass `viewModel.uiEffect` through.

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

#### Scenario: Removing a favourite from the Favourites screen removes it from the displayed list
- **GIVEN** `uiState` is `FavouritesUiState.Content` with a product `id = 7` displayed
- **WHEN** the user taps that product card's heart icon
- **THEN** `FavouritesUiIntent.ToggleFavourite(productId = 7)` is dispatched to `onIntent`

#### Scenario: LoadFavourites intent fires on first composition
- **GIVEN** the stateful `FavouritesScreen()` overload is composed for the first time
- **WHEN** composition completes
- **THEN** `FavouritesUiIntent.LoadFavourites` has been dispatched to the underlying `FavouritesViewModel.onIntent()` exactly once

#### Scenario: A toggle-failure effect shows a snackbar with the error message
- **GIVEN** `FavouritesScreen(uiState, onIntent, uiEffect)` is composed with a `uiEffect` flow that emits `FavouritesUiEffect.ShowFavouriteToggleError`
- **WHEN** that emission is collected
- **THEN** a `Snackbar` displaying the localised `favourite_toggle_error_message` text is shown

### Requirement: Favourite-related strings are localised in English and Spanish
`app/src/main/res/values/strings.xml` SHALL define `favourite_add_content_description`, `favourite_remove_content_description`, `favourite_toggle_error_message`, and `favourites_empty_message` as English (base) string resources, each with a corresponding translated entry in `app/src/main/res/values-es/strings.xml`, and none SHALL appear as a hardcoded literal in source code. Content description strings are resolved in composables via `stringResource()` based on `isFavourite`, not pre-computed in ViewModels.

#### Scenario: Every new string resource has a Spanish translation
- **WHEN** `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml` are compared
- **THEN** `favourite_add_content_description`, `favourite_remove_content_description`, `favourite_toggle_error_message`, and `favourites_empty_message` each exist in both files with a non-empty, distinct Spanish value in `values-es`
