## Context

The app has a single screen today: `MainActivity.onCreate()` calls `setContent { FakeStoreTheme { ProductListScreen(modifier = Modifier.fillMaxSize()) } }`. There is no `NavController`, no `Scaffold` at the activity level, and no navigation dependency in the version catalog at all. `ProductListViewModel` takes only `GetProductsUseCase` in its constructor.

`AnalyticsClient` (in `:core`) is a `CopyOnWriteArrayList<AnalyticsProvider>` dispatcher exposing `fun logEvent(name: String, params: Map<String, Any> = emptyMap())`; `AnalyticsModule` provides it as a Hilt `@Singleton`, registering `DebugAnalyticsProvider` when `BuildConfig.DEBUG`. Nothing in `:app` calls `logEvent` yet.

`ProductListScreen` already has the stateless/stateful overload split (`ProductListScreen(uiState, onIntent, modifier)` for testing, `ProductListScreen(modifier, viewModel = hiltViewModel())` for production) and a `LaunchedEffect(Unit) { currentOnIntent(LoadProducts) }` that fires the initial load exactly once per composition of that screen instance.

This story (1.2.3) introduces the first multi-screen navigation in the app and the first ViewModel-level analytics call sites. It establishes patterns — type-safe routes, a screen-orchestration `MainViewModel`, and the "log-on-success-state" analytics convention — that later stories (product detail, cart) will likely reuse.

Constraints carried over from `docs/guidelines/guidelines-android.md` and user clarifications:
- Composables are purely presentational — navigation triggering and analytics logging are side effects and belong in a ViewModel, exposed to the Composable as `UiIntent` in and `UiEffect` out, never as inline business logic inside a composable.
- MVI: state should make invalid combinations unrepresentable.
- Kover excludes `*.ui.screens.*` and `@Composable`-annotated code — ViewModel logic (tab-tap analytics, `product_list_viewed` logging, selected-tab tracking) is what must hit the 95% bound; Compose UI tests cover the composables separately.
- `detekt.yml` has `warningsAsErrors: true` — new literals (event names, test tag names) must be extracted to named constants.
- No abbreviations — full descriptive names throughout.

## Goals / Non-Goals

**Goals:**
- Give the user a persistent, always-visible 3-tab bottom navigation bar (Products/Favourites/Profile) that preserves scroll position and content state when switching tabs.
- Model tab taps and navigation as an explicit `MainViewModel` MVI triplet so navigation-triggering and analytics logging live in testable ViewModel code, not in composables.
- Log `product_list_viewed` exactly once per successful visit to the Products tab (Content state only), and `tab_products_tapped` / `tab_favourites_tapped` / `tab_profile_tapped` on every tap of the corresponding tab, including re-taps of the already-selected tab.
- Use `@Serializable` type-safe routes (navigation-compose 2.8+) instead of string routes, consistent with the project's existing kotlinx-serialization usage in `:data`.
- Keep `BottomNavigationBar` fully stateless and independently testable via Compose UI tests, with no `NavController` or `ViewModel` dependency.

**Non-Goals:**
- Building out real Favourites or Profile functionality — both are static placeholder screens for this story.
- Deep-linking, nested navigation graphs, or navigation animations/transitions — out of scope.
- Persisting `product_list_viewed` or tab-tap events beyond `DebugAnalyticsProvider`'s Logcat output — no production analytics SDK is wired in this story (unchanged from the existing `AnalyticsModule`).
- Changing `ProductListScreen`'s own Loading/Content/Error rendering — only its ViewModel gains an analytics side effect; the composable is untouched.
- Cross-tab shared state (e.g. a shared cart badge count) — each tab's content is independent.

## Decisions

### 1. `AppDestination` is a sealed interface with three `@Serializable data object` routes, in a new `ui/navigation/` package

```kotlin
package com.guidovezzoni.fakestore.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination {
    @Serializable
    data object Products : AppDestination

    @Serializable
    data object Favourites : AppDestination

    @Serializable
    data object Profile : AppDestination
}
```

Grouping the three route objects in one file follows the existing project precedent of `ProductListUiState.kt` / `ProductListUiIntent.kt`, where every variant of a sealed type lives in the same file as the sealed type itself — the "one class per file" guideline is interpreted, consistent with prior stories, as "one *sealed type and its variants* per file," not one file per `data object`.

`AppDestination` doubles as both the `NavHost`'s route type argument (`composable<AppDestination.Products> { ... }`) and the identifier carried by `MainUiState.selectedDestination`, `MainUiIntent.TabTapped`, and `MainUiEffect.NavigateToTab` — a single type models "which tab/screen" everywhere it is needed, avoiding a parallel enum that would need to be kept in sync with the route objects.

Each object has no parameters — none of the three destinations take navigation arguments in this story.

### 2. `MainUiState` tracks `selectedDestination`; selection is ViewModel state, not derived from `NavController` in the composable

```kotlin
data class MainUiState(
    val selectedDestination: AppDestination = AppDestination.Products,
)
```

*Alternative considered*: derive the highlighted tab directly in `MainScreen` from `navController.currentBackStackEntryAsState()`, as shown in the official Navigation-Compose bottom-bar sample. Rejected — while this is the more common Compose-only pattern, it means `BottomNavigationBar`'s "which tab is selected" fact lives outside `UiState`, so a Compose UI test of `BottomNavigationBar` in isolation (per the project's "no ViewModel mocking" testing guideline) would have no state to assert against without hand-rolling a `NavController`. Making `selectedDestination` part of `MainUiState` keeps `BottomNavigationBar` a pure function of `(selectedDestination, onTabTap)`, directly matching the "Test the composable in isolation: pass `uiState` directly" testing guideline, and keeps the ViewModel-testable surface (which tab is "current") in the ViewModel where Kover measures coverage.

`MainViewModel.onTabTapped()` unconditionally updates `selectedDestination` to the tapped tab (even if unchanged) — idempotent, no branching needed.

### 3. Tab taps flow through `MainUiIntent.TabTapped`; the ViewModel logs analytics and emits a `MainUiEffect.NavigateToTab` that `MainScreen` turns into an actual `NavController.navigate()` call

```kotlin
sealed interface MainUiIntent {
    data class TabTapped(val destination: AppDestination) : MainUiIntent
}

sealed interface MainUiEffect {
    data class NavigateToTab(val destination: AppDestination) : MainUiEffect
}
```

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val analyticsClient: AnalyticsClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<MainUiEffect>()
    val uiEffect: SharedFlow<MainUiEffect> = _uiEffect.asSharedFlow()

    fun onIntent(intent: MainUiIntent) {
        when (intent) {
            is MainUiIntent.TabTapped -> onTabTapped(intent.destination)
        }
    }

    private fun onTabTapped(destination: AppDestination) {
        analyticsClient.logEvent(eventNameFor(destination))
        _uiState.value = _uiState.value.copy(selectedDestination = destination)
        viewModelScope.launch {
            _uiEffect.emit(MainUiEffect.NavigateToTab(destination))
        }
    }

    private fun eventNameFor(destination: AppDestination): String = when (destination) {
        AppDestination.Products -> EVENT_TAB_PRODUCTS_TAPPED
        AppDestination.Favourites -> EVENT_TAB_FAVOURITES_TAPPED
        AppDestination.Profile -> EVENT_TAB_PROFILE_TAPPED
    }

    private companion object {
        const val EVENT_TAB_PRODUCTS_TAPPED = "tab_products_tapped"
        const val EVENT_TAB_FAVOURITES_TAPPED = "tab_favourites_tapped"
        const val EVENT_TAB_PROFILE_TAPPED = "tab_profile_tapped"
    }
}
```

Analytics logging happens unconditionally on every `TabTapped` intent, before the state update — this satisfies "logged on every tap, including the already-selected tab" without any equality check against the current `selectedDestination`.

`MainScreen` collects `uiEffect` and performs the real navigation:

```kotlin
LaunchedEffect(navController) {
    uiEffect.collect { effect ->
        when (effect) {
            is MainUiEffect.NavigateToTab -> navController.navigate(effect.destination) {
                popUpTo(navController.graph.id) { saveState = true; inclusive = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}
```

`popUpTo(navController.graph.id) { inclusive = true }` clears the entire back stack before adding the new destination, so each tab holds a single back stack entry. Pressing back from any tab exits the app rather than navigating between tabs. `saveState = true` / `restoreState = true` still preserves each tab's scroll position and content state across switches.

Because `launchSingleTop = true`, calling `navigate()` with the currently-displayed destination is a no-op with respect to the back stack and composition — this is what makes "tapping the already-selected tab has no visible effect" (AC) true, while analytics still fires on every tap per Decision 3 above (the `logEvent` call happens in the ViewModel regardless of whether the subsequent navigation is a no-op).

### 4. `BottomNavigationBar` is a standalone, fully stateless composable

```kotlin
@Composable
fun BottomNavigationBar(
    selectedDestination: AppDestination,
    onTabTap: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = selectedDestination == AppDestination.Products,
            onClick = { onTabTap(AppDestination.Products) },
            icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
            label = { Text(stringResource(R.string.global_tab_products)) },
            modifier = Modifier.testTag(BOTTOM_NAVIGATION_PRODUCTS_TAB_TEST_TAG),
        )
        NavigationBarItem(
            selected = selectedDestination == AppDestination.Favourites,
            onClick = { onTabTap(AppDestination.Favourites) },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            label = { Text(stringResource(R.string.global_tab_favourites)) },
            modifier = Modifier.testTag(BOTTOM_NAVIGATION_FAVOURITES_TAB_TEST_TAG),
        )
        NavigationBarItem(
            selected = selectedDestination == AppDestination.Profile,
            onClick = { onTabTap(AppDestination.Profile) },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text(stringResource(R.string.global_tab_profile)) },
            modifier = Modifier.testTag(BOTTOM_NAVIGATION_PROFILE_TAB_TEST_TAG),
        )
    }
}
```

This composable takes no `NavController` and no `ViewModel` — it is testable exactly like `ProductListItemCard`, via Compose UI tests that pass a plain `AppDestination` and a capturing lambda. `icon` uses the same `Icons.Filled.*` painter regardless of `selected`; Material 3's `NavigationBarItem` applies `selectedIconColor`/`unselectedIconColor`/`selectedTextColor`/`unselectedTextColor` from the current `ColorScheme` automatically based on the `selected` boolean, satisfying "same filled icon for both states, color-only distinction via Material 3 theming" without any manual color logic in the composable. `contentDescription = null` is intentional — the adjacent `label` already provides an accessible text description for the tab (avoiding duplicate TalkBack announcements); the label itself is the source of truth for both visible text and accessibility.

### 5. `MainScreen` owns the `NavController` and `NavHost`; it is the one composable in this story that cannot be fully "stateless" in the strict `(uiState, onIntent, modifier)` sense

```kotlin
@Composable
fun MainScreen(
    uiState: MainUiState,
    onIntent: (MainUiIntent) -> Unit,
    uiEffect: Flow<MainUiEffect>,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    LaunchedEffect(navController) {
        uiEffect.collect { effect ->
            when (effect) {
                is MainUiEffect.NavigateToTab -> navController.navigate(effect.destination) {
                    popUpTo(navController.graph.id) { saveState = true; inclusive = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomNavigationBar(
                selectedDestination = uiState.selectedDestination,
                onTabTap = { onIntent(MainUiIntent.TabTapped(it)) },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Products,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable<AppDestination.Products> { ProductListScreen() }
            composable<AppDestination.Favourites> { FavouritesScreen() }
            composable<AppDestination.Profile> { ProfileScreen() }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MainScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        uiEffect = viewModel.uiEffect,
        modifier = modifier,
    )
}
```

`rememberNavController()` is unavoidable Compose-owned state — a `NavController` cannot be hoisted into `MainUiState` (it is not a plain, testable data value, and `NavHost` requires a live controller instance bound to the composition). This is the same category of exception `ProductListScreen`'s stateful overload already accepts for `hiltViewModel()`: the stateless overload of `MainScreen` is "stateless" with respect to `MainUiState`/`MainUiIntent`/`MainUiEffect` (the ViewModel-owned contract), not with respect to every piece of Compose-framework state. Compose UI tests for `MainScreen` therefore exercise it end-to-end with a real `NavController` (asserting which screen's content is displayed after simulated taps) rather than trying to stub navigation out.

`startDestination = AppDestination.Products` satisfies "Products tab selected by default at launch," matching `MainUiState`'s own default.

### 6. `product_list_viewed` is logged inside `ProductListViewModel.loadProducts()`, not via a new `UiIntent`

```kotlin
@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val analyticsClient: AnalyticsClient,
) : ViewModel() {

    // ...

    private fun loadProducts() {
        val locale = Locale.getDefault()
        _uiState.value = ProductListUiState.Loading
        viewModelScope.launch {
            getProductsUseCase().collect { result ->
                result
                    .onSuccess { products ->
                        _uiState.value = ProductListUiState.Content(
                            products.map { mapToProductListItem(it, locale) }
                        )
                        analyticsClient.logEvent(EVENT_PRODUCT_LIST_VIEWED)
                    }
                    .onFailure {
                        _uiState.value = ProductListUiState.Error
                    }
            }
        }
    }

    private companion object {
        const val EVENT_PRODUCT_LIST_VIEWED = "product_list_viewed"
    }
}
```

*Alternative considered*: add `ProductListUiIntent.ScreenViewed`, dispatched from a `LaunchedEffect(uiState)` in `ProductListScreen` when `uiState is Content`. Rejected — it introduces a second intent whose entire purpose is to fire once whenever `Content` is reached, duplicating information the ViewModel already has at the exact moment it transitions to `Content`. Logging directly in `loadProducts()`'s `onSuccess` branch is simpler, requires no new intent type, and is trivially unit-testable with a mocked `AnalyticsClient` — no Compose UI test is needed to prove the "once per Content, never on Error" behaviour.

This design relies on the existing `LaunchedEffect(Unit) { currentOnIntent(LoadProducts) }` in `ProductListScreen` (unchanged by this story) to supply the "once per visit" semantics: because `MainScreen`'s `NavHost` fully removes a tab's composable subtree from composition when navigating away (even with `saveState = true` on the back stack entry, the composable itself leaves the composition), returning to the Products tab re-enters `ProductListScreen`'s composition from scratch, so `LaunchedEffect(Unit)` fires again, `LoadProducts` is dispatched again, and `product_list_viewed` is logged again — one call per visit, exactly matching the clarified requirement "re-entering the tab after switching away counts as a new visit." Within a single visit, `loadProducts()` only re-executes on a further intent dispatch (e.g. a hypothetical retry), so recomposition alone never re-logs the event.

An empty successful response (`Content(products = emptyList())`) still logs the event — Decision 6's `onSuccess` branch runs regardless of list size, matching the clarification that empty content counts as a successful visit.

### 7. Version catalog and build file additions

`gradle/libs.versions.toml`:
```toml
[versions]
navigationCompose = "2.9.8"

[libraries]
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
```

`app/build.gradle.kts`:
```kotlin
plugins {
    // ...existing plugins...
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // ...existing dependencies...
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
```

`org.jetbrains.kotlin.plugin.serialization` is already declared in the root version catalog (`libs.plugins.kotlin.serialization`, used today by `:data`) and needs no new catalog entry — only a new `alias(...)` line in `app/build.gradle.kts`'s `plugins {}` block, since `@Serializable` on `AppDestination`'s route objects requires the plugin applied to the module that compiles them. `kotlinx-serialization-json` is likewise already a catalog entry (`libs.kotlinx.serialization.json`, used by `:data`); `:app` gains a direct `implementation` dependency on it since it does not currently depend on `:data` transitively exposing that artifact as `api`.

## Risks / Trade-offs

- **[Risk]** `MainScreen`'s stateless overload still owns `rememberNavController()` internally, so it is not testable purely by passing primitives the way `ProductListScreen`'s stateless overload is. → **Mitigation**: Decision 5 documents this as an accepted, bounded exception (the same category as `hiltViewModel()` in other stateful overloads); `BottomNavigationBar` — the composable with the actual tab-selection/tap-dispatch logic subject to the acceptance criteria — remains fully stateless and unit-testable in isolation. `MainScreen` itself is covered by an instrumented test asserting end-to-end tab-switch behaviour with a real `NavController`.
- **[Risk]** Relying on "leaving a `NavHost` destination removes its composable from composition, so `LaunchedEffect(Unit)` re-fires on return" is an implementation detail of Navigation-Compose's `saveState`/`restoreState` mechanics, not something this design can enforce structurally. → **Mitigation**: this is the exact mechanism named in the user's own clarification #5; the Final Verification section in `tasks.md` includes an on-device check (switch away from Products, switch back, confirm a second `product_list_viewed` Logcat line) to catch any drift from this assumption on the actual Navigation-Compose version pinned in Decision 7.
- **[Design decision]** `MainUiState.selectedDestination` is the single source of truth for which tab is highlighted. Because `popUpTo(navController.graph.id) { inclusive = true }` clears the back stack on every tab switch, back navigation always exits the app rather than navigating between tabs. This eliminates the two-source-of-truth risk: `uiState.selectedDestination` can never diverge from the NavController's current destination via a back press. Future stories adding programmatic navigation (e.g. deep-linking) must also dispatch a state-syncing intent to keep this invariant.
- **[Risk]** Analytics tests for `MainViewModel.uiEffect` (a `SharedFlow` with no replay) require a collector to be actively subscribed before the emitting intent is dispatched, or the emitted value is missed. → **Mitigation**: `tasks.md` specifies the collect-then-dispatch test pattern explicitly (launch a collecting coroutine into a `mutableListOf` before calling `onIntent`), consistent with how `kotlinx-coroutines-test`'s `UnconfinedTestDispatcher` is already used elsewhere in this codebase.

## Migration Plan

No data migration. Sequencing (captured in `tasks.md`): version catalog + build file changes and `AppDestination` first (prerequisites, since every other new file imports one or both), then the `MainViewModel` MVI triplet BDD pairs (tab-tap analytics + state update + effect emission), then `BottomNavigationBar` BDD pairs (rendering + tap dispatch, isolated Compose UI tests), then the `ProductListViewModel` `product_list_viewed` BDD pairs, then `MainScreen`/`FavouritesScreen`/`ProfileScreen` wiring and `MainActivity` integration, then strings + Spanish translations, then final verification against every Acceptance Criterion.

## Open Questions

None outstanding — tab-analytics architecture, route definition style, icon strategy, state-preservation mechanism, re-fire-per-visit semantics, back-button behaviour, and empty-content logging were all resolved via user clarification before this design was written.
