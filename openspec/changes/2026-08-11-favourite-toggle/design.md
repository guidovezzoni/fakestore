## Context

The FakeStore app has a working Products screen (`ProductListScreen`/`ProductListViewModel`, stories 1.2.1–1.2.3) built on MVI + Clean Architecture, and a placeholder `FavouritesScreen` (a single centred `Text`, no ViewModel). Favouriting is entirely new: there is no local persistence anywhere in the project, `ProductListUiEffect` is an empty sealed interface with no emitted instance, and no screen wires a `SnackbarHost` to a `UiEffect` flow yet. `MainScreen` already demonstrates the one pattern this story needs to reuse for effects — a stateless composable that takes `uiEffect: Flow<...>` as an explicit parameter and a `LaunchedEffect` that collects it (see `MainScreen.kt`).

Constraints carried over from `docs/guidelines/guidelines-android.md`, the user story (`docs/userstories/2.1.1-Favourite-Toggle-WIP.md`), and resolved clarifications:
- Room stores **only** `productId: Int`; no other product fields are persisted.
- `FavouritesRepository`/use cases return `Set<Int>` at the domain boundary (repository converts Room's `Flow<List<Int>>` to `Flow<Set<Int>>`).
- Only `GetFavouriteIdsUseCase` is created in this story (no `GetFavouritesUseCase`).
- Room schema starts at version 1, no prior schema, no `fallbackToDestructiveMigration`.
- KSP for Room is added directly to `data/build.gradle.kts` (mirrors `:app`'s existing KSP+Hilt setup).
- `:domain` remains free of Room, Android, and Hilt imports (existing `hilt-dependency-injection` and `product-catalogue-data` spec constraints).
- Analytics call happens after a successful DB write, in the ViewModel (not inside the use case) — consistent with how `ProductListViewModel` already calls `AnalyticsClient` itself rather than delegating that call into `GetProductsUseCase`.

## Goals / Non-Goals

**Goals:**
- A working Room database as the project's first local persistence layer, migration-friendly from day one (schema export enabled) even though no migration exists yet.
- Favourite state visible and toggleable from both the Products and Favourites screens, kept in sync via Room's reactive `Flow`.
- Optimistic toggle with revert-on-failure and a snackbar, without corrupting `ProductListUiState.Content`'s existing Loading/Content/Error contract.
- `favourite_added`/`favourite_removed` analytics fired exactly once per successful toggle, never on failure.
- A fully-functional `FavouritesScreen` (not a placeholder) following the exact same MVI shape as `ProductListScreen`.

**Non-Goals:**
- Toggle animation/visual polish (explicitly deferred in the user story's NFR table).
- Caching/sharing the product list between `ProductListViewModel` and `FavouritesViewModel` — each independently calls `GetProductsUseCase()`. Flagged as a Medium NFR risk in the user story and explicitly accepted for this scope; a follow-up story may introduce a shared/cached product source.
- Database migrations — version 1 only, no `Migration` objects.
- A `GetFavouritesUseCase` returning full `Product` domain models — downstream stories can add this; this story's `GetFavouriteIdsUseCase` returns ids only.
- Any change to the remote API surface (`ApiService`, `ProductRepository`) — favourites are local-only.

## Decisions

### 1. Room artifacts and version
Add `androidx.room:room-runtime`, `androidx.room:room-ktx` (for `Flow`-returning DAO queries), and `androidx.room:room-compiler` (KSP) to the version catalog under a single `room` version reference. `:data` already applies the `fakestore.android.library` convention plugin (it's an Android library module), so Room's Android/SQLite dependency fits without a new convention plugin. `:data`'s `build.gradle.kts` gains `alias(libs.plugins.ksp)` (mirroring `:app`'s existing KSP usage for Hilt).

*Note for implementer*: pin the exact `room` version by checking the latest stable release at implementation time (Maven Central / `google()`) rather than trusting a version guessed during proposal-writing, per the project's data-modelling discipline of not speculating on unconfirmed values — the same caution applies to pinning a library version in a fast-moving toolchain.

### 2. Entity/DAO shape: `Int` list at the DAO boundary, `Set<Int>` at the domain boundary
`FavouriteEntity(@PrimaryKey val productId: Int)`, table name `favourite_entity`. `FavouriteDao` exposes:
- `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entity: FavouriteEntity)`
- `@Delete suspend fun delete(entity: FavouriteEntity)`
- `@Query("SELECT productId FROM favourite_entity") fun getAllIds(): Flow<List<Int>>`

Room's native reactive-query return type is `Flow<List<Int>>` (not `Set`), so the DAO stays idiomatic Room. `FavouritesRepositoryImpl` is the single place that maps `Flow<List<Int>>` → `Flow<Set<Int>>` via `.map { it.toSet() }`, giving `:domain` and the ViewModels O(1) `contains()` checks per the resolved clarification.

*Alternative considered*: a DAO query returning `Flow<Set<Int>>` directly. Rejected — Room does not natively support `Set` as a query return collection type without a custom `TypeConverter`; mapping in the repository is simpler and keeps the DAO boundary idiomatic.

### 3. `ToggleFavouriteUseCase` takes an explicit target state, not "current state lookup"
`ToggleFavouriteUseCase(private val repository: FavouritesRepository)` exposes `suspend operator fun invoke(productId: Int, shouldBeFavourite: Boolean): Result<Unit>`, wrapping `repository.addFavourite(productId)` (when `shouldBeFavourite`) or `repository.removeFavourite(productId)` (otherwise) in `runCatching`. The caller (ViewModel) — which already knows the current `isFavourite` flag on the `ProductListItem` being toggled — computes and passes the *desired* new state, rather than the use case re-deriving "current state" from a fresh repository read (which would introduce a race between the read and the eventual write, and would need to run inside the same use case call as the write).

*Alternative considered*: `invoke(productId: Int)` with the use case internally reading `getFavouriteIds()` first to decide add-vs-remove. Rejected — adds a redundant read (the ViewModel's `uiState` already reflects current membership), and introduces a TOCTOU race between that internal read and the write for no benefit.

`Result<Unit>` (not a richer error type) is sufficient: the ViewModel only needs success/failure to decide whether to keep the optimistic state or revert it, and per the "no exception detail surfaced to the user" pattern already established for `ProductListUiState.Error`, no exception message/class ever reaches the UI.

### 4. `GetFavouriteIdsUseCase` is a thin pass-through
`GetFavouriteIdsUseCase(private val repository: FavouritesRepository)` exposes `operator fun invoke(): Flow<Set<Int>> = repository.getFavouriteIds()`. No transformation beyond what the repository already does — this mirrors the thinness of `ProductRepository`/`GetProductsUseCase`'s existing relationship where the use case's value is the DI seam and testability boundary, not extra logic.

### 5. `ProductListViewModel`: `combine()` a one-shot product fetch with a reactive favourites `Flow`
`ProductListViewModel` gains a private `MutableStateFlow<List<Product>?>` (`rawProducts`, `null` until the first successful fetch). A `combine()` collector, launched once in `init { }`:

```kotlin
init {
    viewModelScope.launch {
        combine(rawProducts.filterNotNull(), getFavouriteIdsUseCase()) { products, favouriteIds ->
            products.map { product ->
                val isFavourite = favouriteIds.contains(product.id)
                mapToProductListItem(
                    product = product,
                    locale = Locale.getDefault(),
                    isFavourite = isFavourite,
                )
            }
        }.collect { items -> _uiState.value = ProductListUiState.Content(items) }
    }
}
```

`loadProducts()` (triggered by `LoadProducts`/`RetryClicked`) is unchanged in spirit — it sets `Loading`, invokes `getProductsUseCase()`, and on `Result.success` sets `rawProducts.value = products` (which triggers the `combine` collector above rather than setting `uiState` directly) and logs `product_list_viewed`; on `Result.failure` it sets `uiState.value = ProductListUiState.Error` directly, bypassing the combine path (an `Error` state is not derived from `rawProducts`/favourite data).

Because `getFavouriteIdsUseCase()` is a live Room-backed `Flow`, this collector re-fires — recomputing `Content` — every time the favourites table changes, independent of which screen (Products or Favourites) performed the write. This is what gives "consistent favourite state across screens" (an explicit acceptance criterion) without either screen polling or manually notifying the other.

*Alternative considered*: keep `ProductListViewModel`'s existing one-shot `loadProducts()` mapping and separately patch `isFavourite` into `uiState` imperatively on every toggle and on a one-time favourites read. Rejected — this reintroduces exactly the kind of manual two-screen synchronisation Room's reactive `Flow` is meant to remove, and would silently go stale if the Favourites screen toggled a product while Products was in the back stack (`saveState = true` keeps its composition alive per the `bottom-navigation` capability).

### 6. Optimistic toggle + revert-on-failure is self-healing via the same `combine()` path
`onIntent(ToggleFavourite(productId))`:
1. Reads the current `ProductListUiState.Content.products` (no-op if `uiState` isn't `Content` — a toggle can't be dispatched from a state with no rendered items).
2. Computes `newIsFavourite = !item.isFavourite` and immediately writes an optimistically-updated `ProductListUiState.Content` — this is the "immediate visual feedback" acceptance criterion.
3. Launches a coroutine calling `toggleFavouriteUseCase(productId, newIsFavourite)`.
4. On success: logs `favourite_added` or `favourite_removed` (selected by `newIsFavourite`) with `params = mapOf("product_id" to productId)`.
5. On failure: restores the pre-toggle `products` list (the snapshot captured in step 1) and emits `ProductListUiEffect.ShowFavouriteToggleError` via `_uiEffect`.

Because step 2 writes directly to `_uiState` (not to `rawProducts`), it does not disturb the `combine()` collector's upstream inputs. When the write in step 3 succeeds, Room's `Flow` (consumed by the `init` collector) will itself re-emit shortly after and recompute the *same* `Content` value from `rawProducts` + fresh `favouriteIds` — the optimistic write and the reactive recompute converge on the same state. This is deliberately treated as "self-healing" rather than a bug: even if the manual optimistic write in step 2 were skipped, Room's own emission would eventually catch up (just with a visible delay), and if step 2 and the reactive recompute overlap, they agree.

*Risk*: a narrow window exists where the DB write fails (triggering the revert in step 5) at the same moment the `combine()` collector re-fires from an unrelated favourites-table change (e.g. the other screen's own toggle). Given Room writes and this app's data volume (single-user, on-device, ~20 products), this is a low-probability, low-impact race — worst case, a stale item briefly shows the wrong favourite icon until the next recompute. Not mitigated further in this story; flagged here for visibility rather than silently accepted.

### 7. Analytics logging stays in the ViewModel, not the use case
`ToggleFavouriteUseCase` has no `AnalyticsClient` dependency and lives in `:domain`, which has no Gradle dependency on `:core` (confirmed in `product-catalogue-data`'s "Module dependency graph is enforced" requirement — `:domain` depends on nothing). `ProductListViewModel`/`FavouritesViewModel` already hold `AnalyticsClient` (an existing constructor dependency on `ProductListViewModel`) and are the layer responsible for side effects in this codebase's established MVI convention — `product-list-screen`'s existing `product_list_viewed` logging follows the same shape. Keeping analytics in the ViewModel also makes "no event on failure" trivial to guarantee: it's simply never called on the `onFailure` branch.

### 8. Content description strings are resolved in the composable via `stringResource()`
The favourite toggle icon's content description ("Add to favourites" / "Remove from favourites") is resolved inside the composable rather than pre-computed in the ViewModel. `ProductListItemCard` calls `stringResource(if (item.isFavourite) R.string.favourite_remove_content_description else R.string.favourite_add_content_description)` — the same kind of presentational branching already used to select between `Icons.Filled.Favorite` and `Icons.Outlined.FavoriteBorder`. This keeps both ViewModels free of `@ApplicationContext Context`, avoids introducing a `FavouriteContentDescriptionProvider` utility, and removes the `favouriteContentDescription: String` field from `ProductListItem` entirely.

*Alternative considered*: pre-compute `favouriteContentDescription` in the ViewModel via `@ApplicationContext Context`, exposing it as a `ProductListItem` field. Rejected — introduces Android `Context` into ViewModels for the first time (no precedent in this codebase), adds a utility class and its tests, and the string selection is purely presentational (it maps 1:1 from `isFavourite`, with no business logic involved).

### 9. `FavouritesViewModel` mirrors `ProductListViewModel`'s shape, filtered to favourited IDs
`FavouritesViewModel` independently calls `getProductsUseCase()` (one-shot) and `getFavouriteIdsUseCase()` (reactive), `combine()`s them the same way, but maps only products whose `id` is present in `favouriteIds` (every resulting `ProductListItem.isFavourite` is therefore always `true`). Tapping the heart icon on the Favourites screen always means "remove" — `toggleFavouriteUseCase(productId, shouldBeFavourite = false)` — with the same optimistic-removal/revert-on-failure/snackbar shape as `ProductListViewModel`, applied to filtering the item out of the locally-held list rather than flipping a boolean field.

`FavouritesUiState` reuses the exact three-variant shape already established by `ProductListUiState` (`Loading` / `Content(products: List<ProductListItem>)` / `Error`), so `FavouritesScreen` can reuse `ProductListItemCard` directly for rendering, and the "empty state" acceptance criterion is satisfied the same way `ProductListScreen` already handles it — `Content(products = emptyList())` renders a dedicated empty message, no new sealed variant needed.

*Alternative considered*: derive Favourites content purely from `ProductListViewModel`'s already-fetched data (shared/hoisted state) instead of `FavouritesViewModel` independently calling `GetProductsUseCase()`. Rejected for this story — it would mean either promoting product-list state above both ViewModels (a bigger architectural change than this story's scope) or having `FavouritesViewModel` reach into `ProductListViewModel` (breaks ViewModel isolation). Accepted as a documented Non-Goal; the NFR table in the user story already flags the duplicate fetch as an accepted Medium-risk trade-off.

### 10. Snackbar plumbing: stateless screens gain an explicit `uiEffect: Flow<...>` parameter
Both `ProductListScreen` and `FavouritesScreen`'s stateless composables gain a `uiEffect: Flow<ProductListUiEffect>` / `Flow<FavouritesUiEffect>` parameter (defaulting to `emptyFlow()` so existing/new `@Preview`s don't need to supply one), collected via `LaunchedEffect(snackbarHostState) { uiEffect.collect { ... } }` that calls `snackbarHostState.showSnackbar(...)` on the failure effect. This exactly mirrors the pattern `MainScreen` already established for `MainUiEffect` (a stateless composable taking `uiEffect: Flow<MainUiEffect>` and collecting it) — this story is simply the second consumer of that pattern, and the first one used for a `Scaffold`'s `snackbarHost` slot specifically. Each `Scaffold` gains `snackbarHost = { SnackbarHost(snackbarHostState) }`; the error message string is `stringResource(R.string.favourite_toggle_error_message)`.

*Alternative considered*: a `Channel`-based one-shot mechanism read directly from the stateful overload only (never passed into the stateless composable). Rejected — the project's own Compose UI testing guideline requires testing the stateless composable in isolation via `uiState` + `onIntent`; adding `uiEffect` as an explicit parameter (rather than something only reachable through `hiltViewModel()`) is what makes "the snackbar appears on `ShowFavouriteToggleError`" testable without mocking a ViewModel, consistent with existing `MainScreen`/`ProductListScreen` test conventions.

### 11. `DatabaseModule`: first `@ApplicationContext` usage, Room provisioning
A new `@Module @InstallIn(SingletonComponent::class) object DatabaseModule` in `app/di/`:

```kotlin
@Provides
@Singleton
fun provideFavouritesDatabase(@ApplicationContext context: Context): FavouritesDatabase =
    Room.databaseBuilder(context, FavouritesDatabase::class.java, DATABASE_NAME).build()

@Provides
fun provideFavouriteDao(database: FavouritesDatabase): FavouriteDao = database.favouriteDao()

@Provides
fun provideFavouritesRepository(favouriteDao: FavouriteDao): FavouritesRepository =
    FavouritesRepositoryImpl(favouriteDao)

@Provides
fun provideToggleFavouriteUseCase(repository: FavouritesRepository): ToggleFavouriteUseCase =
    ToggleFavouriteUseCase(repository)

@Provides
fun provideGetFavouriteIdsUseCase(repository: FavouritesRepository): GetFavouriteIdsUseCase =
    GetFavouriteIdsUseCase(repository)
```

The database is `@Singleton`-scoped (one connection for the app's lifetime, matching `NetworkClient`/`AnalyticsClient`'s existing scoping); the DAO/repository/use case providers are unscoped, following `DataModule`'s existing convention where only the underlying client/database is `@Singleton` and everything built from it is provided fresh (cheap to construct, no shared mutable state of its own). `DatabaseModuleTest` follows the exact `DataModuleTest` pattern — calling `DatabaseModule.provideX(...)` directly with fakes/mocks, no Hilt graph boot required, asserting the concrete return type (e.g. `provideFavouritesRepository(mockDao) is FavouritesRepositoryImpl`) or delegation behaviour.

`Room` schema export is enabled (`exportSchema = true` on `@Database`, with `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` in `data/build.gradle.kts`), even though version 1 has no migration to test yet — this keeps the database "migration-friendly from day one" per the user story's NFR, rather than retrofitting schema export when the second migration story arrives.

### 12. `ProductListItemMapper` gains one new parameter, existing tests extend rather than break
`mapToProductListItem(product: Product, locale: Locale, isFavourite: Boolean): ProductListItem` — the new parameter is required (not defaulted), so every existing call site is forced to pass a real value, preventing a silent `isFavourite = false` default from masking a missed wiring point. `ProductListItemMapperTest`'s existing six tests are updated to pass a fixed `isFavourite` argument; one new test asserts the field maps straight through unchanged.

## Risks / Trade-offs

- **[Risk]** First Room usage in the project — KSP annotation processing errors (e.g. a missing `@PrimaryKey`, an unsupported `Flow`-of-`Set` return type) will only surface at compile time. → **Mitigation**: Decision 2 above deliberately avoids `Flow<Set<Int>>` at the DAO layer; `FavouriteEntity`/`FavouriteDao`/`FavouritesDatabase` are prerequisite tasks built and compiled before any dependent use case/repository code is written (see `tasks.md` §3).
- **[Risk]** The `combine()` + optimistic-update interplay (Decision 5–6) is the most structurally complex ViewModel logic in the project so far. → **Mitigation**: `ProductListViewModelTest` explicitly covers the merge scenario (favourite ids arriving after products), the optimistic-then-success path, and the optimistic-then-failure-then-revert path as distinct test cases, per the user story's Unit Testing table.
- **[Trade-off]** `FavouritesViewModel` re-fetches the full product list from the network independently of `ProductListViewModel` (Decision 9). → Accepted per the user story's own NFR table; noted as a Non-Goal above so it isn't mistaken for an oversight during review.
- **[Trade-off]** No `Migration` objects or `room-testing` dependency added in this story, since there is no prior schema to migrate from. → Schema export is still enabled (Decision 11) so the first future migration has a version-1 baseline to diff against.

## Migration Plan

No data migration (first Room database, version 1, `exportSchema = true`, no `fallbackToDestructiveMigration`). Sequencing (captured in `tasks.md`): version catalog + `:data` KSP wiring first, then `FavouriteEntity`/`FavouriteDao`/`FavouritesDatabase` (structural prerequisites, Room's generated code has no meaningful unit-test surface of its own), then `FavouritesRepositoryImpl` (BDD, mocked DAO), then the two domain use cases (BDD), then `ProductListItem`/mapper extension (BDD), then `ProductListUiIntent`/`ProductListUiEffect` additions (prerequisites), then `ProductListViewModel`'s combine/toggle rework (BDD), then `ProductListItemCard`'s heart icon and `ProductListScreen`'s snackbar (BDD, Compose UI tests), then the full `FavouritesScreen` MVI buildout (BDD), then DI wiring (`DatabaseModule`, BDD via direct-call tests), then strings/previews (integration), then final verification (`detektDebug`, `test`, `connectedDebugAndroidTest`, `koverHtmlReportDebug`, on-device check per `guidelines-process.md`).

## Open Questions

None outstanding — return type (`Set<Int>`), scope (full Favourites screen per the DoD), use-case naming (`GetFavouriteIdsUseCase` only), KSP placement, content description resolution (composable-side via `stringResource()`), and migration strategy were all resolved via user clarification before this design was written.
