## Why

The app currently has exactly one screen: `MainActivity` composes `ProductListScreen` directly inside `FakeStoreTheme`, with no `Scaffold`, no bottom navigation, and no way to reach any other area of the app. As the catalogue grows into a full shopping experience, users need a persistent way to move between Products, Favourites, and Profile without losing their place. At the same time, the product owner has no visibility into catalogue engagement — nothing is logged when a user actually sees the product list, and there is no signal for which navigation tabs users interact with. The `AnalyticsClient`/`AnalyticsProvider` abstraction already exists in `:core` (delivered in story 1.1.x) but nothing in `:app` calls it yet beyond DI wiring.

## What Changes

- Introduce a persistent Material 3 bottom navigation bar with three tabs — Products, Favourites, Profile — hosted by a new `MainScreen` composable that owns a `NavHost` with type-safe (`@Serializable`) routes. `MainActivity` is updated to render `MainScreen()` instead of `ProductListScreen()` directly.
- Add two new placeholder screens, `FavouritesScreen` and `ProfileScreen`, each a centred, stateless, purely presentational composable showing a "coming soon" message sourced from `strings.xml`.
- Introduce a full MVI triplet for the new screen-level concern: `MainViewModel`, `MainUiState`, `MainUiIntent`, `MainUiEffect`. Tab taps are modelled as `MainUiIntent.TabTapped`; the ViewModel logs the corresponding tap analytics event on every tap (including re-tapping the already-selected tab) and emits a `MainUiEffect.NavigateToTab` effect that `MainScreen` consumes to drive the actual `NavController.navigate()` call, using the standard `popUpTo(startDestination) { saveState = true }` + `restoreState = true` + `launchSingleTop = true` pattern so switching tabs preserves scroll position and content state, and re-tapping the selected tab is a visual no-op.
- Add `product_list_viewed` analytics logging to `ProductListViewModel`: fired exactly once per successful fetch that reaches `ProductListUiState.Content` (empty or populated), never on `Loading` or `Error`. `ProductListViewModel` gains a new `AnalyticsClient` constructor dependency.
- Add three new string resources for tab labels (`global_tab_products`, `global_tab_favourites`, `global_tab_profile`) and two for the placeholder screens (`favourites_placeholder`, `profile_placeholder`), each with an English (base) and Spanish translation.
- Add `androidx.navigation:navigation-compose` (2.9.8) to the version catalog and `:app`'s dependencies, plus the Kotlin serialization Gradle plugin and `kotlinx-serialization-json` runtime dependency to `:app` (needed to compile `@Serializable` route types).

## Capabilities

### New Capabilities
- `bottom-navigation`: persistent 3-tab Material 3 bottom navigation bar (Products/Favourites/Profile) with type-safe routing, state-preserving tab switches, a `MainViewModel` MVI triplet, and per-tap `tab_*_tapped` analytics events.

### Modified Capabilities
- `product-list-screen`: `ProductListViewModel` gains an `AnalyticsClient` dependency and logs `product_list_viewed` exactly once per successful load that reaches `Content` state.

## Impact

- **New source code** (`:app`): `ui/navigation/AppDestination.kt`, `ui/screens/MainScreen.kt`, `ui/screens/BottomNavigationBar.kt`, `ui/screens/FavouritesScreen.kt`, `ui/screens/ProfileScreen.kt`, `ui/viewmodel/MainViewModel.kt`, `ui/state/MainUiState.kt`, `ui/intent/MainUiIntent.kt`, `ui/effect/MainUiEffect.kt`.
- **Modified source code** (`:app`): `MainActivity.kt` (renders `MainScreen()` instead of `ProductListScreen()`), `ui/viewmodel/ProductListViewModel.kt` (adds `AnalyticsClient` dependency and `product_list_viewed` logging), `res/values/strings.xml`, `res/values-es/strings.xml`.
- **Modified build files**: `gradle/libs.versions.toml` (add `navigationCompose` version + `androidx-navigation-compose` library), `app/build.gradle.kts` (add `kotlin.serialization` plugin, `androidx.navigation.compose` and `kotlinx.serialization.json` dependencies).
- **Modified test code**: `app/src/test/.../ui/viewmodel/ProductListViewModelTest.kt` (extend `createViewModel(...)` factory with an `AnalyticsClient` param; add analytics assertions).
- **New test code**: `app/src/test/.../ui/viewmodel/MainViewModelTest.kt`, `app/src/androidTest/.../ui/screens/BottomNavigationBarTest.kt`, `app/src/androidTest/.../ui/screens/MainScreenTest.kt`.
- **No changes** to `:domain`, `:data`, `:core` — `AnalyticsClient`/`AnalyticsProvider`/`DebugAnalyticsProvider` and `AnalyticsModule` already exist and need no modification; this story only adds new call sites in `:app`.
