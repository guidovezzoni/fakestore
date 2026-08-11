## Why

The product list screen (delivered in story 1.2.1) currently renders `uiState.products` unconditionally: while the network call is in flight the screen is blank, a failed call is silently swallowed (`ProductListViewModel.loadProducts()` has no `onFailure` branch at all today), and an empty product list renders as an empty scrollable area with no explanation. Users have no way to distinguish "still loading" from "nothing to show" from "something broke." This story closes that gap by giving the screen three explicit, mutually exclusive visual states.

## What Changes

- Restructure `ProductListUiState` from a flat `data class` into a `sealed interface` with exactly three variants: `Loading` (no fields), `Content(products: List<ProductListItem>)`, and `Error` (no message field — see `design.md` Decision 1 for the deliberate deviation from the user story's AC-8).
- Add `ProductListUiIntent.RetryClicked`, a `data object` sibling to the existing `LoadProducts`, semantically distinct even though it drives the same fetch behaviour today.
- Update `ProductListViewModel`: initial state becomes `ProductListUiState.Loading` (no auto-fetch in `init{}` — the screen still triggers the first fetch via its existing `LaunchedEffect(Unit)`); `loadProducts()` now sets `Loading` before invoking the use case, `Content` (mapped products, including the empty-list case) on success, and `Error` on failure. `RetryClicked` routes to the same `loadProducts()` function.
- Update `ProductListScreen`: the `Scaffold` content becomes a `when` over the sealed `uiState`, rendering a centred `CircularProgressIndicator` for `Loading`; for `Content`, either the existing `LazyColumn` of cards or an empty-state message depending on whether `products` is empty; and for `Error`, an error message (resolved via `stringResource()` at render time) with a "Retry" button dispatching `RetryClicked`. The `TopAppBar` stays outside the `when`, unconditionally visible (AC-11).
- Add three new string resources (`product_list_error_message`, `product_list_retry_button`, `product_list_empty_message`) to `values/strings.xml` and their Spanish translations to `values-es/strings.xml`.
- Update every existing `ProductListViewModelTest` and `ProductListScreenTest` case for the sealed `UiState` shape, and add new tests for loading, error, empty, and retry behaviour.

## Capabilities

### Modified Capabilities
- `product-list-screen`: `ProductListUiState` becomes a sealed type with `Loading`/`Content`/`Error` variants (was a flat data class); `ProductListUiIntent` gains `RetryClicked`; `ProductListViewModel` gains loading/error/retry handling with a documented (no-guard) approach to rapid retries; `ProductListScreen` renders state-specific UI while keeping the top app bar always visible.

## Impact

- **Modified source code** (`:app`): `ui/state/ProductListUiState.kt`, `ui/intent/ProductListUiIntent.kt`, `ui/viewmodel/ProductListViewModel.kt`, `ui/screens/ProductListScreen.kt`, `res/values/strings.xml`, `res/values-es/strings.xml`.
- **Modified test code**: `app/src/test/.../ui/viewmodel/ProductListViewModelTest.kt`, `app/src/androidTest/.../ui/screens/ProductListScreenTest.kt`.
- **No changes** to `:domain`, `:data`, `:core` — `GetProductsUseCase` already returns `Flow<Result<List<Product>>>` with `catch`; `ApiService` and `NetworkClient` timeouts are already in place.
- **Breaking change (internal only)**: any code constructing `ProductListUiState(products = ...)` (existing tests, previews) must move to `ProductListUiState.Content(products = ...)`. No public/external API is affected.
