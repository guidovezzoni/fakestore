## Why

Story 2.1.1 shipped a fully-functional `FavouritesScreen` (state/intent/effect/ViewModel/composable) but deliberately left its `favourites_screen_viewed` analytics event and an actionable empty-state message out of scope. Story 2.2.1 closes those two gaps — without a `favourites_screen_viewed` event, engagement with the Favourites feature is unmeasurable, and the current empty-state text ("No favourites yet") doesn't tell a first-time visitor how to add a favourite. The screen's Error state is also currently untested despite already being implemented, which this change closes as a coverage gap.

## What Changes

- Add `TrackScreenViewed` to `FavouritesUiIntent`, dispatched by `FavouritesScreen` from the same `LaunchedEffect(Unit)` that already dispatches `LoadFavourites` on first composition.
- `FavouritesViewModel` handles `TrackScreenViewed` by launching a coroutine that collects `uiState`, waits for the first `Content` emission, and logs `favourites_screen_viewed` with a `favourite_count` (Int) parameter equal to `products.size` at that moment — mirroring `ProductListViewModel`'s existing `product_list_viewed` pattern of only logging once data is available, never from `Loading` or `Error`.
- Update `favourites_empty_message` in `values/strings.xml` from "No favourites yet" to "No favourites yet. Tap the heart on a product to save it.", with the corresponding Spanish translation updated in `values-es/strings.xml`.
- Add unit tests to `FavouritesViewModelTest` covering `TrackScreenViewed` dispatched against `Content` (populated and empty), `Loading`, and `Error` states, plus a double-dispatch case.
- Add Compose UI tests to `FavouritesScreenTest` covering `TrackScreenViewed` dispatch on composition, the updated empty-state message text, and the previously-untested `Error` state rendering.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `favourite-toggle`: `FavouritesUiIntent` gains `TrackScreenViewed`; `FavouritesViewModel` gains a `favourites_screen_viewed` analytics requirement (logged once Content is reached, with `favourite_count`, never on `Loading`/`Error`); `FavouritesScreen` dispatches the new intent on first composition; the `favourites_empty_message` string requirement is updated to reflect the new instructional copy.

## Impact

- **Modified source code** (`:app`): `ui/intent/FavouritesUiIntent.kt`, `ui/viewmodel/FavouritesViewModel.kt`, `ui/screens/FavouritesScreen.kt`, `res/values/strings.xml`, `res/values-es/strings.xml`.
- **Modified test code**: `app/src/test/.../ui/viewmodel/FavouritesViewModelTest.kt`, `app/src/androidTest/.../ui/screens/FavouritesScreenTest.kt`.
- **No new dependencies, no domain/data-layer changes, no navigation changes.**
- **No breaking changes** — `TrackScreenViewed` is an additive sealed-interface entry; `FavouritesUiState`/`FavouritesUiEffect` are unchanged; existing `LoadFavourites`/`ToggleFavourite` behaviour is untouched.
