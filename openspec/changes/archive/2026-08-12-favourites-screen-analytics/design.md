## Context

`FavouritesScreen`/`FavouritesViewModel`/`FavouritesUiState`/`FavouritesUiIntent`/`FavouritesUiEffect` already exist end-to-end from story 2.1.1 (`favourite-toggle` capability). `FavouritesViewModel` already injects `AnalyticsClient` and calls `logEvent()` for `favourite_removed` inside `toggleFavourite()`. The sibling `ProductListViewModel` already implements the exact analytics pattern this story needs to replicate for Favourites: `product_list_viewed` is logged either synchronously (if `uiState` is already `Content` when the intent arrives) or from inside `loadProducts()`'s `onSuccess` callback (if a fetch is in flight). `FavouritesViewModel`'s `init {}` block already runs a `combine(rawProducts.filterNotNull(), getFavouriteIdsUseCase())` collector that continuously updates `uiState` to `Content` — including from favourites-table changes that happen with no new intent dispatched at all (e.g. a removal made from the Products screen). This last point rules out a direct reuse of `ProductListViewModel`'s exact synchronous-check-or-callback shape for `TrackScreenViewed`.

## Goals / Non-Goals

**Goals:**
- Log `favourites_screen_viewed` (with `favourite_count`) exactly once per `TrackScreenViewed` dispatch, once `Content` is available — never from `Loading` or `Error`.
- Reuse the established `LaunchedEffect(Unit)` → intent → ViewModel-logs-on-data-available shape from `product_list_viewed`, adapted to `FavouritesViewModel`'s `combine()`-based state production.
- Close the two known test gaps: no `FavouritesScreenTest` coverage for `Error` state, no analytics test coverage at all for the Favourites screen.
- Update `favourites_empty_message` (both locales) to give first-time visitors actionable guidance.

**Non-Goals:**
- No change to `FavouritesUiState`, `FavouritesUiEffect`, or the existing `LoadFavourites`/`ToggleFavourite` handling.
- No change to how `favourite_removed` is logged (already correct, already tested).
- No new use case, repository method, or domain model — this is purely an `:app`-layer analytics/string change.
- No debouncing or de-duplication of repeated `TrackScreenViewed` dispatches — each dispatch is treated as a distinct screen-entry event, consistent with the user story's explicit "dispatched twice -> logged twice" scenario.

## Decisions

### 1. `trackScreenViewed()` launches a coroutine that collects `uiState`, filtered to `Content`, and logs on the first emission

`FavouritesViewModel`'s existing `init {}` collector already owns writing `Content` to `uiState` — including re-firing when Room's favourite-ids flow changes independently of any intent. A synchronous snapshot check (`if (_uiState.value is Content) logEvent(...) else <do nothing, no callback to hook into>`) would silently drop the event on the common first-visit path, where `TrackScreenViewed` and `LoadFavourites` are dispatched together from the same `LaunchedEffect(Unit)` while `uiState` is still `Loading`. There is no `loadFavourites()`-owned success callback to piggyback on the way `ProductListViewModel.loadProducts()` does, because `Content` is produced by the independent `combine()` collector, not by `loadFavourites()` itself.

`trackScreenViewed()` therefore does:

```kotlin
private fun trackScreenViewed() {
    viewModelScope.launch {
        val content = uiState.filterIsInstance<FavouritesUiState.Content>().first()
        analyticsClient.logEvent(
            name = EVENT_FAVOURITES_SCREEN_VIEWED,
            params = mapOf(PARAM_FAVOURITE_COUNT to content.products.size),
        )
    }
}
```

Each call to `trackScreenViewed()` launches its own independent collector that waits for (or, if `Content` is already current, immediately resolves against) the next `Content` emission and logs once, then completes — it does not stay subscribed. Two dispatches of `TrackScreenViewed` launch two independent collectors and therefore log twice, matching the user story's explicit "dispatched twice -> logged twice" acceptance scenario. `Error` never satisfies `filterIsInstance<Content>()`, so an in-flight `trackScreenViewed()` collector launched before an `Error` transition simply never completes/logs for that dispatch — this is the mechanism by which "no event in Error state" holds, not an explicit `is Error -> return` branch.

*Alternative considered*: synchronous snapshot check mirroring `ProductListViewModel.loadProductsOrTrack()` exactly (`if (Content) log else loadFavourites()`). Rejected — `TrackScreenViewed` and `LoadFavourites` are two separate intents dispatched independently (per the existing `FavouritesUiIntent` shape and the user story's explicit intent addition), so `trackScreenViewed()` has no reason to also trigger a fetch, and doing a synchronous check would miss the always-Loading-at-first-composition case entirely.

*Alternative considered*: hoist the logging into the `init{}` `combine()` collector itself (log on every `Content` emission, gated by a `hasLoggedThisComposition` flag reset by `TrackScreenViewed`). Rejected — couples the reactive Room-driven state pipeline to a UI-visit-counting concern, is harder to test in isolation (would require exercising the shared collector rather than a dedicated method), and doesn't map as cleanly to "each dispatch is a distinct screen-entry event."

### 2. `favourite_count` is read from the `Content` snapshot that satisfied the collector, not from a fresh `uiState.value` read

Using the `content` value captured by `.first()` (rather than re-reading `_uiState.value` after the suspend point) avoids a benign but confusing race: if the favourites table changes between the `Content` emission that unblocked `.first()` and a subsequent `_uiState.value` read, the logged count could silently diverge from "the state that triggered this log call." Capturing the emitted value keeps the reported `favourite_count` traceable to the exact emission that caused the event to fire.

### 3. Companion object constants follow the existing `favourite_removed` pattern

`EVENT_FAVOURITES_SCREEN_VIEWED = "favourites_screen_viewed"` and `PARAM_FAVOURITE_COUNT = "favourite_count"` are added to `FavouritesViewModel`'s existing `private companion object`, alongside `EVENT_FAVOURITE_REMOVED`/`PARAM_PRODUCT_ID` — no new file, consistent with the "literals extraction" guideline and the existing companion's shape.

### 4. `FavouritesScreen` dispatches `TrackScreenViewed` from the same `LaunchedEffect(Unit)` as `LoadFavourites`, not a second effect

```kotlin
LaunchedEffect(Unit) {
    currentOnIntent(FavouritesUiIntent.LoadFavourites)
    currentOnIntent(FavouritesUiIntent.TrackScreenViewed)
}
```

Both dispatches share the same "once per composition" lifecycle guarantee `LaunchedEffect(Unit)` already provides for `LoadFavourites`; a second `LaunchedEffect(Unit)` block would be redundant (same key, same one-shot semantics) and would only obscure that the two intents are dispatched together on screen entry.

### 5. Unit tests pre-seed `Content` before dispatching `TrackScreenViewed`

Because the test suite uses `UnconfinedTestDispatcher`, a `.first()`-based collector launched via `viewModelScope.launch` resolves synchronously once its upstream `Flow` already holds a matching value at launch time. Tests reach `Content` first (via `LoadFavourites`, as existing tests already do), then dispatch `TrackScreenViewed` and assert `analyticsClient.logEvent(...)` synchronously — no `runCurrent()`/`advanceUntilIdle()` beyond what `UnconfinedTestDispatcher` already provides. The `Loading`/`Error` scenarios dispatch `TrackScreenViewed` without ever reaching `Content` and assert `logEvent` is never invoked with the new event name.

## Risks / Trade-offs

- **[Risk]** A `TrackScreenViewed` collector launched while `uiState` is `Error` never completes and is never cancelled except by `viewModelScope`'s own lifecycle (i.e. `onCleared()`), leaking one suspended coroutine per dispatch made during an error state. → **Mitigation**: `viewModelScope` is cancelled when the ViewModel is cleared, so the leaked coroutine's lifetime is bounded by the ViewModel's own lifetime — the same bound every other `viewModelScope.launch` call in this codebase already has. Not treated as a correctness bug; flagged for visibility given it's a slightly different shape (a `.first()` that may never satisfy) than the rest of the codebase's launches.
- **[Trade-off]** Each `TrackScreenViewed` dispatch launches its own collector rather than sharing one subscription, so N dispatches during a session hold up to N pending coroutines simultaneously in the worst case (e.g. repeated dispatches while stuck in `Error`). → Accepted: `LaunchedEffect(Unit)` dispatches `TrackScreenViewed` at most once per composition in the shipped `FavouritesScreen`, so this only matters for direct `onIntent()` calls (e.g. tests) or a future caller that dispatches it repeatedly; the user story explicitly wants repeated dispatches to log repeatedly, which rules out a single shared/de-duplicated subscription.

## Migration Plan

No runtime migration. Sequencing (captured in `tasks.md`): `FavouritesUiIntent.TrackScreenViewed` (prerequisite, other tests import it) first, then `FavouritesViewModel`'s `trackScreenViewed()` (BDD), then `FavouritesScreen`'s dispatch wiring and the `Error`-state Compose test (BDD), then the `favourites_empty_message` string update in both locales (integration), then final verification (`detektDebug`, `test`, `connectedDebugAndroidTest`, `koverHtmlReportDebug`, on-device check per `guidelines-process.md`).

## Open Questions

None outstanding — analytics timing (collect-until-Content vs. synchronous snapshot), Spanish translation wording, and empty-state accessibility were all resolved via user clarification before this design was written.
