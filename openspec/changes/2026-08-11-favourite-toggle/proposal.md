## Why

The FakeStore app currently has no way for a user to mark a product as a favourite, and the Favourites tab (added in story 1.2.3) is still a placeholder Text composable. This story delivers the first write path in the app — favouriting is local-only (Room), optimistic in the UI, and reversible on failure — and turns the Favourites tab into a real screen. It also establishes the project's first Room database, the first `combine()`-based reactive ViewModel pattern, and the first real usage of `ProductListUiEffect`/snackbar plumbing.

## What Changes

- Add a Room database (`:data`) with a single `favourite_entity` table storing only `productId: Int` (primary key) — no other product fields are persisted locally.
- Add `FavouritesRepository` (interface in `:domain`, `FavouritesRepositoryImpl` in `:data`) exposing `addFavourite(productId)`, `removeFavourite(productId)`, and `getFavouriteIds(): Flow<Set<Int>>` (converted from Room's native `Flow<List<Int>>` for O(1) membership checks downstream).
- Add `ToggleFavouriteUseCase(productId: Int, shouldBeFavourite: Boolean): Result<Unit>` and `GetFavouriteIdsUseCase(): Flow<Set<Int>>` in `:domain`, both plain Kotlin with no Room/Android dependency.
- Extend `ProductListItem` with `isFavourite: Boolean`, computed in the ViewModel.
- Extend `ProductListUiIntent` with `ToggleFavourite(productId: Int)` and `ProductListUiEffect` with `ShowFavouriteToggleError`, the app's first real one-shot effect.
- Rework `ProductListViewModel` to combine the one-shot product fetch with the reactive `GetFavouriteIdsUseCase()` flow via `combine()`, so favourite state updates automatically when Room changes. Toggling a favourite updates `uiState` optimistically, calls `ToggleFavouriteUseCase`, logs `favourite_added`/`favourite_removed` (with `product_id`) only after a successful write, and reverts the optimistic change plus emits `ShowFavouriteToggleError` on failure.
- Add a heart `IconButton` (filled/outlined, content description resolved via `stringResource()` based on `isFavourite`) to `ProductListItemCard`, and wire a `SnackbarHost` + effect collection into `ProductListScreen`.
- Replace the `FavouritesScreen` placeholder with a full MVI feature: `FavouritesUiState`/`FavouritesUiIntent`/`FavouritesUiEffect`/`FavouritesViewModel`, displaying only favourited products (fetched via `GetProductsUseCase` and filtered against `GetFavouriteIdsUseCase`'s ids), an empty-state message when there are none, and the same optimistic-toggle/revert/snackbar behaviour as the Products screen.
- Add Room (`room-runtime`, `room-ktx`, `room-compiler` via KSP) to `gradle/libs.versions.toml` and `:data`'s build file — the first KSP annotation processing in `:data`.
- Add a new `DatabaseModule` (`:app/di/`) provisioning the Room database, DAO, `FavouritesRepository`, and the two new use cases; extend `DataModule`'s existing test coverage pattern for the new provisions.
- Add four new strings (`favourite_add_content_description`, `favourite_remove_content_description`, `favourite_toggle_error_message`, `favourites_empty_message`) to `values/strings.xml` and their Spanish translations in `values-es/strings.xml`.

## Capabilities

### New Capabilities
- `favourite-toggle`: Local favourite persistence end-to-end — the Room schema, `FavouritesRepository`/`FavouritesRepositoryImpl`, `ToggleFavouriteUseCase`/`GetFavouriteIdsUseCase`, the `favourite_added`/`favourite_removed` analytics contract, and the full `FavouritesScreen` MVI feature (state/intent/effect/ViewModel/composable).

### Modified Capabilities
- `product-list-screen`: `ProductListItem`, `ProductListUiIntent`, `ProductListUiEffect`, `ProductListViewModel`, `ProductListItemCard`, and `ProductListScreen` all gain favourite-toggle behaviour (heart icon, optimistic update, snackbar on failure).
- `bottom-navigation`: the combined "FavouritesScreen and ProfileScreen show centred placeholder messages" requirement is narrowed to ProfileScreen only — FavouritesScreen's real behaviour is now owned by the `favourite-toggle` capability.

## Impact

- **New source code** (`:domain`): `repository/FavouritesRepository.kt`, `usecase/ToggleFavouriteUseCase.kt`, `usecase/GetFavouriteIdsUseCase.kt`.
- **New source code** (`:data`): `database/FavouriteEntity.kt`, `database/FavouriteDao.kt`, `database/FavouritesDatabase.kt`, `repository/FavouritesRepositoryImpl.kt`.
- **New source code** (`:app`): `di/DatabaseModule.kt`; `ui/state/FavouritesUiState.kt`, `ui/intent/FavouritesUiIntent.kt`, `ui/effect/FavouritesUiEffect.kt`, `ui/viewmodel/FavouritesViewModel.kt`.
- **Modified source code** (`:app`): `ui/state/ProductListItem.kt`, `ui/intent/ProductListUiIntent.kt`, `ui/effect/ProductListUiEffect.kt`, `ui/viewmodel/ProductListViewModel.kt`, `ui/util/ProductListItemMapper.kt`, `ui/screens/ProductListItemCard.kt`, `ui/screens/ProductListScreen.kt`, `ui/screens/FavouritesScreen.kt`, `di/DataModule.kt` (unchanged contents, but `DataModuleTest`-style tests extend to `DatabaseModuleTest`), `values/strings.xml`, `values-es/strings.xml`.
- **New test code**: `ToggleFavouriteUseCaseTest`, `GetFavouriteIdsUseCaseTest`, `FavouritesRepositoryImplTest`, `DatabaseModuleTest`, `FavouritesViewModelTest`, `FavouritesScreenTest` (androidTest); updates to `ProductListViewModelTest`, `ProductListItemMapperTest`, `ProductListItemCardTest` (androidTest).
- **New dependencies**: `androidx.room:room-runtime`, `androidx.room:room-ktx`, `androidx.room:room-compiler` (KSP) — first Room usage in the project; KSP added to `:data`'s build file (already present in `:app`).
- **No API changes** — favourites remain local-only; `ApiService`/`ProductRepository` are unchanged.
- **No breaking changes to existing behaviour** — `ProductListScreen`'s Loading/Content/Error states and existing formatting/analytics behaviour are preserved; the change is additive (new intent/effect variants, new optional-with-default `ProductListItem` fields).
