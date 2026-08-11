## Context

The product list screen was delivered in story 1.2.1 as a single-state MVI screen: `ProductListUiState` is a flat data class holding only `products: List<ProductListItem>`, defaulting to `emptyList()`. `ProductListViewModel.loadProducts()` collects `getProductsUseCase()` and updates `uiState.products` on `Result.onSuccess` — there is no `onFailure` branch at all, so a network failure is silently swallowed and the screen simply stays on whatever it last showed (typically the initial empty list, rendered as a blank scrollable area). There is also no loading indicator, so during the fetch the screen renders as a blank area indistinguishable from "no products" or "failed."

This story (1.2.2) introduces the first sealed `UiState` in the codebase, the first `CircularProgressIndicator`/retry-button pattern, and establishes how loading/error/empty states are represented and tested going forward — patterns later screens (favourites, product detail) will likely follow.

Constraints carried over from `docs/guidelines/guidelines-android.md` and user clarifications:
- MVI: `UiState` should make invalid states unrepresentable — a sealed type ensures "loading" and "showing content" cannot coexist.
- Composables are purely presentational — resolving a string via `stringResource()` at render time is allowed (it is not business logic, it is the standard Compose way to source display text), but no locale detection, formatting, or business branching belongs in the composable.
- `detekt.yml` has `warningsAsErrors: true` — new literals (test tag names, etc.) must be extracted to named constants.
- Kover excludes `*.ui.screens.*` and `@Composable`-annotated code — `ProductListViewModel` is the component whose new branches must hit the 95% bound.

## Goals / Non-Goals

**Goals:**
- Give the user unambiguous visual feedback for exactly three states: loading, error (with retry), and content (populated or empty).
- Make invalid state combinations (e.g. "loading" while also "showing products") structurally impossible via a sealed `ProductListUiState`.
- Keep the error message generic and localised, with no technical leakage (exception names, HTTP codes, stack traces).
- Establish the loading-indicator / retry-button pattern for future screens.

**Non-Goals:**
- Rapid-retry / concurrent-fetch guarding (e.g. cancelling in-flight requests, `collectLatest`) — deferred to a later story per user clarification. `RetryClicked` and `LoadProducts` both call a plain `viewModelScope.launch`, so rapid taps could in theory launch overlapping fetches; accepted as a known, documented gap.
- Carrying a message string inside the `Error` state — deliberately deviates from the user story's AC-8 (see Decision 1).
- Auto-fetch from `ViewModel.init{}` — the screen still triggers the first fetch itself via `LaunchedEffect(Unit)`, unchanged from 1.2.1.
- Pull-to-refresh, caching, offline-first behaviour — out of scope, noted as informational in the user story's NFRs.

## Decisions

### 1. `Error` carries no message field — the Composable resolves the string at render time

The user story's AC-8 states "`Error` holds a user-facing error message string." This design deliberately diverges: `ProductListUiState.Error` is a parameterless `data object`. The error text (`stringResource(R.string.product_list_error_message)`) is resolved inside `ProductListScreen` when rendering the `Error` branch, not stored on the state.

Rationale: `stringResource()` can only be called from a `@Composable` context. For the ViewModel to hold a resolved string, it would need a `Context`/`Resources` reference or a new `ResourceProvider` DI abstraction purely to satisfy a field that today has exactly one possible value. That is unwarranted complexity for a single generic error message. If a future story needs different error copy for different failure types (e.g. "no connectivity" vs "server error"), `Error` can grow a discriminating field then (e.g. `Error(val reason: ErrorReason)`) and the Composable can still do the string lookup — this decision does not block that evolution.

*Alternative considered*: `Error(val message: String)`, with the ViewModel hardcoding a literal English string. Rejected — this would violate the localisation requirement (AC-10) by baking an English-only string into the ViewModel, defeating the purpose of `values-es/strings.xml`.

*Alternative considered*: introduce a `ResourceProvider` interface (`fun getString(@StringRes id: Int): String`) injected into the ViewModel, letting it emit `Error(message = resourceProvider.getString(R.string.product_list_error_message))`. Rejected for this story — it is the "correct" long-term pattern for a ViewModel that needs localised strings, but introduces a new DI seam and interface for a single call site. Revisit if a second ViewModel needs the same capability.

**Deviation flagged for story sign-off**: this reduces the literal fidelity of AC-8; behaviourally the user still sees a generic, localised, user-friendly error message, satisfying AC-3, AC-4, and AC-10.

### 2. `ProductListUiState` becomes a `sealed interface`, not a `sealed class`

Matches existing project convention: `ProductListUiIntent` and `ProductListUiEffect` are already `sealed interface`s in this codebase, even though `guidelines-android.md`'s example skeleton shows `sealed class`. Consistency with sibling MVI types wins over the guideline example.

```kotlin
sealed interface ProductListUiState {
    data object Loading : ProductListUiState
    data class Content(val products: List<ProductListItem>) : ProductListUiState
    data object Error : ProductListUiState
}
```

### 3. Initial state is `Loading`; no auto-fetch in `init{}`

Per user clarification: `_uiState = MutableStateFlow(ProductListUiState.Loading)` — the ViewModel starts in `Loading` but does not itself call `loadProducts()` in `init{}`. The existing `LaunchedEffect(Unit) { onIntent(LoadProducts) }` in `ProductListScreen` remains the sole trigger for the first fetch, unchanged from 1.2.1. This keeps the ViewModel free of side effects at construction time (constructing a `ProductListViewModel` in a test never triggers a network call unless the test explicitly dispatches an intent) while still satisfying the Definition of Done's "ViewModel initial state is `Loading`."

### 4. `RetryClicked` reuses `loadProducts()` verbatim

```kotlin
fun onIntent(intent: ProductListUiIntent) {
    when (intent) {
        is ProductListUiIntent.LoadProducts, is ProductListUiIntent.RetryClicked -> loadProducts()
    }
}
```

They are kept as distinct sealed entries (not collapsed into one) because they represent different user-facing triggers — the initial screen load vs. an explicit user action — which is useful context for future analytics instrumentation (the codebase already has an `AnalyticsClient` abstraction) even though the current behaviour is identical.

### 5. `loadProducts()` always sets `Loading` first, then `Content`/`Error`

```kotlin
private fun loadProducts() {
    val locale = Locale.getDefault()
    _uiState.value = ProductListUiState.Loading
    viewModelScope.launch {
        getProductsUseCase().collect { result ->
            result
                .onSuccess { products ->
                    _uiState.value = ProductListUiState.Content(products.map { mapToProductListItem(it, locale) })
                }
                .onFailure {
                    _uiState.value = ProductListUiState.Error
                }
        }
    }
}
```

This satisfies AC-5 ("tapping Retry re-triggers the fetch and transitions back to loading") and closes the previously-missing `onFailure` branch. A success with an empty list naturally produces `Content(emptyList())` — no special-casing needed; the empty-state branching happens entirely in the Composable (Decision 6).

### 6. Empty state is a rendering branch within `Content`, not a fourth sealed variant

Per AC-9 and the user story's explicit instruction ("distinguished ... within `Content`"), `ProductListScreen`'s `when` has three branches (`Loading`, `Content`, `Error`), and inside the `Content` branch, `if (state.products.isEmpty())` renders the empty-state message instead of the `LazyColumn`. A fourth `Empty` sealed variant was considered and rejected — it would duplicate `Content`'s shape (an empty list already models "no products" without extra state) and contradicts the story's explicit AC-9 wording.

### 7. `TopAppBar` stays outside the `when`

`ProductListScreen`'s `Scaffold(topBar = { TopAppBar(...) })` is unchanged structurally from 1.2.1 — only the `content` lambda body becomes a `when (uiState)`. This trivially satisfies AC-11 (top app bar visible in all states) because the `TopAppBar` is never inside the branching logic.

### 8. New test tag constants for the loading/error/empty regions

Following the existing `PRODUCT_LIST_ITEM_CARD_TEST_TAG_PREFIX` pattern in `ProductListItemCard.kt`, `ProductListScreen.kt` gains new top-level test tag constants (`PRODUCT_LIST_LOADING_INDICATOR_TEST_TAG`, `PRODUCT_LIST_ERROR_CONTAINER_TEST_TAG`, `PRODUCT_LIST_RETRY_BUTTON_TEST_TAG`, `PRODUCT_LIST_EMPTY_MESSAGE_TEST_TAG`) so Compose UI tests assert on structural elements without depending on translatable string content.

## Risks / Trade-offs

- **[Risk]** Diverging from AC-8 (`Error` message field) could read as an unfulfilled acceptance criterion during story verification. → **Mitigation**: Decision 1 documents this as a deliberate, user-approved deviation; behaviourally AC-3/AC-4/AC-10 are still satisfied (a generic, localised, non-technical message is shown). The verification step should note this explicitly rather than mark AC-8 as a plain pass or fail.
- **[Risk]** Rapid Retry taps launch overlapping coroutines (no cancellation guard); each can independently update `_uiState`, so the last one to complete wins — a stale `Error` could overwrite a later `Content` (or vice versa) if responses arrive out of order. → **Accepted** per user clarification; deferred to a later story. Documented in the user story's own Performance section as a known, low-risk gap given OkHttp's bounded timeouts (10s connect / 30s read).
- **[Trade-off]** Not introducing a `ResourceProvider` keeps this story's scope tight but means the ViewModel cannot unit-test the *content* of the error message (only that the state is `Error`) — AC-4's "no technical details" is instead covered by inspecting the static string resource content, not a ViewModel assertion.
- **[Risk]** Restructuring `ProductListUiState` from a data class to a sealed interface is a breaking change to every existing call site that constructs `ProductListUiState(products = ...)` (previews, tests). → **Mitigation**: `tasks.md`'s BDD pairs update every existing test and preview alongside the type change, in the same task group, so the codebase never sits in a half-migrated state between commits within this change.

## Migration Plan

No data migration. Sequencing (captured in `tasks.md`): sealed `UiState` + `RetryClicked` intent first (prerequisites, since both the ViewModel and Screen tests import them), then `ProductListViewModel` BDD pairs (loading/error/empty/retry), then `ProductListScreen` BDD pairs (loading/error/retry-button/empty/top-app-bar rendering), then strings + Spanish translations, then final verification against every AC and DoD item in `docs/userstories/1.2.2-Product-List-States-WIP.md`.

## Open Questions

None outstanding — error-message ownership, rapid-retry handling, and auto-fetch triggering were all resolved via user clarification before this design was written.
