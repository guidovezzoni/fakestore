# Capability: Bottom Navigation

## Purpose

Provides the app's primary navigation structure: a persistent bottom navigation bar with three tabs (Products, Favourites, Profile), type-safe route definitions, MVI state/intent/effect contracts for tab selection, and state-preserving tab switching via Jetpack Navigation Compose.

## Requirements

### Requirement: AppDestination models the three navigable screens as type-safe routes
`:app` SHALL define `AppDestination` as a `sealed interface` with exactly three `@Serializable data object` members — `Products`, `Favourites`, `Profile` — each carrying no fields. `AppDestination` SHALL serve as the `NavHost`'s route type and as the identifier used by `MainUiState.selectedDestination`, `MainUiIntent.TabTapped`, and `MainUiEffect.NavigateToTab`.

#### Scenario: Each destination is a parameterless serializable data object
- **WHEN** `AppDestination.Products`, `AppDestination.Favourites`, or `AppDestination.Profile` is referenced
- **THEN** each is a `data object` member of the `AppDestination` sealed interface, annotated `@Serializable`, carrying no fields

### Requirement: MainUiState tracks the currently selected destination, defaulting to Products
`:app` SHALL define `MainUiState` as a `data class` with a single field `selectedDestination: AppDestination`, defaulting to `AppDestination.Products`.

#### Scenario: Default state selects Products
- **WHEN** `MainUiState()` is constructed with no arguments
- **THEN** `selectedDestination` equals `AppDestination.Products`

### Requirement: MainUiIntent models a tab tap as a single intent carrying the tapped destination
`:app` SHALL define `MainUiIntent` as a sealed interface with one entry, `TabTapped(val destination: AppDestination)`, dispatched whenever the user taps any bottom navigation tab, including the currently-selected tab.

#### Scenario: TabTapped carries the tapped destination
- **WHEN** `MainUiIntent.TabTapped(AppDestination.Favourites)` is constructed
- **THEN** its `destination` field equals `AppDestination.Favourites`

### Requirement: MainUiEffect models navigation as a one-shot effect
`:app` SHALL define `MainUiEffect` as a sealed interface with one entry, `NavigateToTab(val destination: AppDestination)`, emitted by `MainViewModel` in response to `TabTapped` and consumed exactly once by `MainScreen` to drive the actual `NavController.navigate()` call.

#### Scenario: NavigateToTab carries the destination to navigate to
- **WHEN** `MainUiEffect.NavigateToTab(AppDestination.Profile)` is constructed
- **THEN** its `destination` field equals `AppDestination.Profile`

### Requirement: MainViewModel updates selectedDestination and emits a navigation effect on every tab tap
`MainViewModel.onIntent()` SHALL, on receiving `MainUiIntent.TabTapped(destination)`, unconditionally set `uiState.selectedDestination` to `destination` and emit `MainUiEffect.NavigateToTab(destination)` via `uiEffect`, regardless of whether `destination` equals the currently selected destination.

#### Scenario: Tapping a different tab updates selectedDestination and emits NavigateToTab
- **GIVEN** a `MainViewModel` whose `uiState.selectedDestination` is `AppDestination.Products`
- **WHEN** `MainUiIntent.TabTapped(AppDestination.Favourites)` is dispatched via `onIntent()`
- **THEN** `uiState.value.selectedDestination` equals `AppDestination.Favourites`, and `uiEffect` emits `MainUiEffect.NavigateToTab(AppDestination.Favourites)`

#### Scenario: Re-tapping the already-selected tab still updates state and emits the effect
- **GIVEN** a `MainViewModel` whose `uiState.selectedDestination` is `AppDestination.Products`
- **WHEN** `MainUiIntent.TabTapped(AppDestination.Products)` is dispatched via `onIntent()`
- **THEN** `uiState.value.selectedDestination` remains `AppDestination.Products`, and `uiEffect` emits `MainUiEffect.NavigateToTab(AppDestination.Products)`

### Requirement: MainViewModel logs a distinct analytics event for every tab tap, with no parameters
`MainViewModel` SHALL log `tab_products_tapped`, `tab_favourites_tapped`, or `tab_profile_tapped` via `AnalyticsClient.logEvent()` — selected by the tapped `AppDestination` — on every `MainUiIntent.TabTapped` dispatch, including when the tapped destination equals the currently selected one, always with an empty parameters map.

#### Scenario: Tapping the Products tab logs tab_products_tapped
- **GIVEN** a `MainViewModel` constructed with a mocked `AnalyticsClient`
- **WHEN** `MainUiIntent.TabTapped(AppDestination.Products)` is dispatched via `onIntent()`
- **THEN** the mocked `AnalyticsClient.logEvent()` is invoked exactly once with `name = "tab_products_tapped"` and empty `params`

#### Scenario: Tapping the Favourites tab logs tab_favourites_tapped
- **GIVEN** a `MainViewModel` constructed with a mocked `AnalyticsClient`
- **WHEN** `MainUiIntent.TabTapped(AppDestination.Favourites)` is dispatched via `onIntent()`
- **THEN** the mocked `AnalyticsClient.logEvent()` is invoked exactly once with `name = "tab_favourites_tapped"` and empty `params`

#### Scenario: Tapping the Profile tab logs tab_profile_tapped
- **GIVEN** a `MainViewModel` constructed with a mocked `AnalyticsClient`
- **WHEN** `MainUiIntent.TabTapped(AppDestination.Profile)` is dispatched via `onIntent()`
- **THEN** the mocked `AnalyticsClient.logEvent()` is invoked exactly once with `name = "tab_profile_tapped"` and empty `params`

#### Scenario: Tapping the already-selected tab still logs its event
- **GIVEN** a `MainViewModel` constructed with a mocked `AnalyticsClient`, whose `uiState.selectedDestination` is `AppDestination.Products`
- **WHEN** `MainUiIntent.TabTapped(AppDestination.Products)` is dispatched a second time via `onIntent()`
- **THEN** the mocked `AnalyticsClient.logEvent()` is invoked with `name = "tab_products_tapped"` on each dispatch, once per tap

### Requirement: BottomNavigationBar renders three labelled, iconed tabs and highlights the selected one
`:app` SHALL define a stateless `BottomNavigationBar(selectedDestination: AppDestination, onTabTapped: (AppDestination) -> Unit, modifier: Modifier = Modifier)` composable rendering a Material 3 `NavigationBar` with exactly three `NavigationBarItem`s — Products (`Icons.Filled.ShoppingCart`), Favourites (`Icons.Filled.Favorite`), Profile (`Icons.Filled.Person`) — each labelled via a `strings.xml` resource. Each item's `selected` parameter SHALL be `true` only when `selectedDestination` equals that item's `AppDestination`; the same icon painter SHALL be used regardless of `selected`, relying on Material 3's default `NavigationBarItemColors` to distinguish the selected tab by colour alone. Tapping any item SHALL invoke `onTabTapped` with that item's `AppDestination`, regardless of whether it equals `selectedDestination`.

#### Scenario: All three tabs are displayed with their labels
- **WHEN** `BottomNavigationBar(selectedDestination = AppDestination.Products, onTabTapped = {})` is composed
- **THEN** three `NavigationBarItem`s are displayed, labelled with the localised Products, Favourites, and Profile tab strings

#### Scenario: The selected tab is the one matching selectedDestination
- **GIVEN** `selectedDestination = AppDestination.Favourites`
- **WHEN** `BottomNavigationBar(selectedDestination, onTabTapped = {})` is composed
- **THEN** the Favourites `NavigationBarItem` has `selected = true`, and the Products and Profile items have `selected = false`

#### Scenario: Tapping a tab invokes onTabTapped with that tab's destination
- **GIVEN** `selectedDestination = AppDestination.Products`
- **WHEN** the user taps the Favourites tab
- **THEN** `onTabTapped` is invoked with `AppDestination.Favourites`

#### Scenario: Tapping the already-selected tab still invokes onTabTapped
- **GIVEN** `selectedDestination = AppDestination.Products`
- **WHEN** the user taps the Products tab
- **THEN** `onTabTapped` is invoked with `AppDestination.Products`

### Requirement: MainScreen hosts a persistent bottom navigation bar and a NavHost with state-preserving tab switches
`:app` SHALL define `MainScreen`, a `Scaffold`-based composable whose `bottomBar` is always `BottomNavigationBar` and whose content is a `NavHost` with `startDestination = AppDestination.Products`, containing exactly three destinations — `AppDestination.Products` (hosting `ProductListScreen`), `AppDestination.Favourites` (hosting `FavouritesScreen`), `AppDestination.Profile` (hosting `ProfileScreen`). On receiving `MainUiEffect.NavigateToTab(destination)`, `MainScreen` SHALL call `navController.navigate(destination)` with `popUpTo(startDestination) { saveState = true }`, `launchSingleTop = true`, and `restoreState = true`, so that switching away from and back to a tab restores its prior scroll position and content state rather than reloading it from scratch mid-visit, and so that navigating to the already-displayed destination produces no visible change.

#### Scenario: The bottom navigation bar is visible regardless of the active tab
- **GIVEN** `MainScreen` is composed with any of the three destinations active
- **WHEN** the composition is inspected
- **THEN** `BottomNavigationBar` is displayed in the `Scaffold`'s `bottomBar` slot in every case

#### Scenario: Products is the default destination shown at launch
- **GIVEN** `MainScreen` is composed for the first time
- **WHEN** the initial composition completes
- **THEN** the `NavHost`'s current destination is `AppDestination.Products`, and the Products tab is shown as selected

#### Scenario: Switching to another tab and back restores prior scroll position and content
- **GIVEN** the Products tab is showing a scrolled product list
- **WHEN** the user switches to the Favourites tab and then back to the Products tab
- **THEN** the product list is shown at the same scroll position it was at before switching away, without a fresh loading indicator being shown

#### Scenario: Navigating to the already-displayed destination is a visible no-op
- **GIVEN** the Products tab is currently displayed
- **WHEN** `MainUiEffect.NavigateToTab(AppDestination.Products)` is received
- **THEN** the displayed screen and its scroll position remain unchanged, and no new destination is pushed onto the back stack

#### Scenario: Pressing the system back button from a non-Products tab returns to Products
- **GIVEN** the Favourites or Profile tab is currently displayed
- **WHEN** the user presses the system back button
- **THEN** the Products tab is displayed, following the `popUpTo(startDestination) { saveState = true }` back-stack configuration

#### Scenario: Pressing the system back button from the Products tab exits the app
- **GIVEN** the Products tab is currently displayed and is the only entry remaining below it on the back stack
- **WHEN** the user presses the system back button
- **THEN** the app exits, following standard Android back-stack behaviour for a NavHost's start destination

### Requirement: MainActivity renders MainScreen as its root content
`MainActivity` SHALL render `MainScreen()` (the stateful, `hiltViewModel()`-backed overload) as the sole content of `setContent { FakeStoreTheme { ... } }`, replacing the previous direct `ProductListScreen()` call. Navigation setup (the `NavController`, `NavHost`, and route graph) SHALL be scoped entirely to `:app` — no navigation type or dependency SHALL be introduced into `:core`, `:domain`, or `:data`.

#### Scenario: App launch shows MainScreen with the bottom navigation bar
- **GIVEN** the app is launched fresh
- **WHEN** `MainActivity.onCreate()` completes and initial composition finishes
- **THEN** `MainScreen` is composed as the root content, the bottom navigation bar is visible, and the Products tab (hosting `ProductListScreen`) is the active destination

### Requirement: FavouritesScreen and ProfileScreen show centred placeholder messages
`:app` SHALL define stateless `FavouritesScreen(modifier: Modifier = Modifier)` and `ProfileScreen(modifier: Modifier = Modifier)` composables, each displaying a single centred `Text` sourced from a `strings.xml` resource — "Favourites coming soon…" and "Profile coming soon…" respectively — and no other content.

#### Scenario: FavouritesScreen shows its placeholder message
- **WHEN** `FavouritesScreen()` is composed
- **THEN** a centred text node showing the localised `favourites_placeholder` string is displayed

#### Scenario: ProfileScreen shows its placeholder message
- **WHEN** `ProfileScreen()` is composed
- **THEN** a centred text node showing the localised `profile_placeholder` string is displayed

### Requirement: Tab labels and placeholder strings are localised in English and Spanish
`app/src/main/res/values/strings.xml` SHALL define `global_tab_products`, `global_tab_favourites`, `global_tab_profile`, `favourites_placeholder`, and `profile_placeholder` as English (base) string resources, each with a corresponding translated entry in `app/src/main/res/values-es/strings.xml`, and none SHALL appear as a hardcoded literal in `BottomNavigationBar.kt`, `FavouritesScreen.kt`, or `ProfileScreen.kt`.

#### Scenario: Every new string resource has a Spanish translation
- **WHEN** `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml` are compared
- **THEN** `global_tab_products`, `global_tab_favourites`, `global_tab_profile`, `favourites_placeholder`, and `profile_placeholder` each exist in both files with a non-empty, distinct Spanish value in `values-es`

#### Scenario: No hardcoded literals for the new user-facing strings
- **WHEN** `BottomNavigationBar.kt`, `FavouritesScreen.kt`, and `ProfileScreen.kt` are inspected
- **THEN** every tab label and placeholder message is sourced via `stringResource(R.string.*)`, with no inline string literals passed to `Text` or `label`
