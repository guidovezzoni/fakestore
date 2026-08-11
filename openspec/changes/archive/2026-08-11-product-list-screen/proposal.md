## Why

The FakeStore app currently shows only a placeholder "Hello Android" screen — there is no user-facing way to browse the product catalogue, even though the full data pipeline (`GetProductsUseCase`, delivered in story 1.1.1) and DI wiring (story 1.1.3) are already in place and unused. This story delivers the first real screen of the app: a scrollable product list, unblocking every downstream Product Catalogue story (1.2.2 loading/error states, 1.2.3 bottom navigation, 2.1.1 favourite toggle).

## What Changes

- Add a `ProductListScreen` composable rendering a vertically scrollable `LazyColumn` of product cards (`key = { it.id }`), and a `ProductListItemCard` composable showing image, title, formatted price, and formatted rating score for a single item.
- Add MVI artefacts in `:app`: `ProductListUiState` (`products: List<ProductListItem>`), `ProductListUiIntent` (`LoadProducts`), `ProductListUiEffect` (empty sealed class, structure only), and `ProductListItem` (display-ready model: `id`, `imageUrl`, `title`, `formattedPrice`, `formattedRatingScore`).
- Add `ProductListViewModel` (`@HiltViewModel`), exposing `uiState: StateFlow<ProductListUiState>` and `uiEffect: SharedFlow<ProductListUiEffect>`, receiving intents via `onIntent()`. On `LoadProducts`, it invokes `GetProductsUseCase`, maps each domain `Product` to a `ProductListItem`, formatting price as locale-aware USD currency and rating score as a locale-aware number.
- Add async image loading via Coil 3 (`io.coil-kt.coil3:coil-compose`), with a Material icon placeholder shown while loading and on load failure.
- Replace the placeholder `Greeting("Android")` in `MainActivity` with `ProductListScreen`, obtaining the ViewModel via `hiltViewModel()`.
- Add `androidx-lifecycle-viewmodel-compose`, `hilt-navigation-compose`, and Coil 3 dependencies to `gradle/libs.versions.toml` and `app/build.gradle.kts`.
- Add user-facing strings (screen title, image content description) to `app/src/main/res/values/strings.xml` and a new `app/src/main/res/values-es/strings.xml` with Spanish translations.
- Establish the first ViewModel unit test (JUnit 4 + MockK + coroutines-test) and first Compose UI test (`compose-ui-test-junit4`) patterns for this codebase.

This story covers **content state only** — loading, error, and empty states are deferred to story 1.2.2.

## Capabilities

### New Capabilities
- `product-list-screen`: The product list screen feature end-to-end — MVI contract (`ProductListUiState`/`ProductListUiIntent`/`ProductListUiEffect`), `ProductListViewModel` mapping and formatting behaviour, the `ProductListScreen`/`ProductListItemCard` composables, async image loading behaviour, and `MainActivity` wiring as the app's initial screen.

### Modified Capabilities
- None. `product-catalogue-data` (`:domain`/`:data`/`:core`) is consumed as-is via `GetProductsUseCase`; no requirement changes to that spec.

## Impact

- **New source code** (`:app`): `ui/screens/ProductListScreen.kt`, `ui/screens/ProductListItemCard.kt` (or similarly named card composable), `ui/viewmodel/ProductListViewModel.kt`, `ui/state/ProductListUiState.kt`, `ui/intent/ProductListUiIntent.kt`, `ui/effect/ProductListUiEffect.kt`, a `ProductListItem` display model, and a price/rating formatting utility.
- **New test code**: `ProductListViewModelTest` (unit, `app/src/test/`), a Compose UI test for `ProductListScreen` (`app/src/androidTest/`).
- **Modified files**: `MainActivity.kt` (screen wiring), `gradle/libs.versions.toml` and `app/build.gradle.kts` (Coil 3, lifecycle-viewmodel-compose, hilt-navigation-compose), `app/src/main/res/values/strings.xml`, new `app/src/main/res/values-es/strings.xml`.
- **New dependencies**: Coil 3 (`coil-compose`), `androidx-lifecycle-viewmodel-compose`, `hilt-navigation-compose`.
- **No domain/data-layer changes** — `GetProductsUseCase`, `Product`, `Rating` are consumed unchanged.
- **No breaking changes** — this replaces an unused placeholder screen; nothing existing depends on `Greeting`.
