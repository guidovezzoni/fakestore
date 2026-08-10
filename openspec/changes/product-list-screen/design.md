## Context

The FakeStore app is a single `:app` Android module on top of an already-built, already-tested data pipeline: `:core` (network client), `:domain` (`Product`, `Rating`, `GetProductsUseCase`), `:data` (DTOs, mapper, repository) — delivered in stories 1.1.1–1.1.3 and already wired into Hilt via `DataModule`. `:app` currently has no `ui/screens`, `ui/viewmodel`, `ui/state`, `ui/intent`, `ui/effect` packages, no image-loading library, no `hiltViewModel()`/`viewModel()` Compose integration, and no ViewModel or Compose UI test precedent. `MainActivity` renders a static `Greeting("Android")` placeholder inside a `Scaffold`.

This is the first screen story of the Product Catalogue epic. It establishes patterns (MVI package layout, ViewModel test structure, Compose UI test structure, image loading) that stories 1.2.2 (loading/error/empty states), 1.2.3 (bottom navigation), and 2.1.1 (favourite toggle) build directly on top of — so getting the shape right here avoids rework later.

Constraints carried over from `docs/guidelines/guidelines-android.md` and user clarifications:
- MVI: single `UiState`, sealed `UiIntent`/`UiEffect`, `onIntent()` entry point, `StateFlow`/`SharedFlow` exposure.
- Composables are purely presentational — no formatting/locale logic inside them.
- Formatting/utility functions live in their own file under `ui/util/`, not co-located in a composable file.
- Kover already excludes `*.ui.screens.*` and `@Composable`/`@Preview`-annotated code — the ViewModel and any formatting utility are the components that must hit the 95% bound.
- `detekt.yml` has `warningsAsErrors: true` — all literals (dimensions, format patterns) must be extracted to named constants.
- No new Gradle module: all new files live in `:app` (Assumption 9 in the user story).

## Goals / Non-Goals

**Goals:**
- Replace the placeholder screen with a working `ProductListScreen` showing all 20 products from `GetProductsUseCase`, following MVI end-to-end.
- Establish the `ui/screens`, `ui/viewmodel`, `ui/state`, `ui/intent`, `ui/effect`, `ui/util` package structure per the guidelines, for reuse by every subsequent screen story.
- Async image loading with Coil 3, including the correct network engine dependency so images actually load over HTTPS.
- Locale-aware USD price formatting and locale-aware rating number formatting, computed in the ViewModel, never in a composable.
- First ViewModel unit test (MockK + `runTest`/`UnconfinedTestDispatcher`) and first Compose UI test (`createComposeRule`, isolated `uiState`/`onIntent`) in the repository, at ≥95% Kover coverage on testable code.

**Non-Goals:**
- Loading, error, and empty `UiState` variants — deferred to story 1.2.2. This story's `UiState` has exactly one shape (content).
- Bottom navigation, favourite toggle, product detail navigation, analytics — deferred to 1.2.3, 2.1.1, and later stories respectively.
- A new `:ui` Gradle module for shared Compose components — deferred per Assumption 9; everything lives in `:app`.
- Pagination or infinite scroll — the API returns a flat 20-item list; `ProductListScreen` renders it directly.

## Decisions

### 1. Image loading: Coil 3 with the OkHttp network engine
User-confirmed: `io.coil-kt.coil3:coil-compose`. Coil 3 splits its network layer out of the core artifact — **the `coil-compose` artifact alone cannot load network images**; a network engine artifact is required. Since `:app`/`:core` already depend on OkHttp (via Retrofit), `io.coil-kt.coil3:coil-network-okhttp` is added alongside `coil-compose` rather than the Ktor engine, avoiding a second HTTP client stack. Both are added as version-catalog entries sharing a single `coil` version reference.

*Alternative considered*: `coil-network-ktor3`. Rejected — this is an Android-only app (not Compose Multiplatform), so there is no reason to introduce Ktor as a second networking dependency alongside the existing OkHttp/Retrofit stack.

### 2. Image placeholder/error: Material icon painter, not a custom drawable
User-confirmed: a simple Material icon (e.g. `Icons.Default.Image` via `rememberVectorPainter`) passed to `AsyncImage`'s `placeholder` and `error` parameters. This avoids adding new drawable assets and keeps the story's scope to code, matching "Image placeholder: Simple Material icon placeholder (not a custom drawable)".

### 3. No new `:ui` module — all new code in `:app`
Per the story's Assumption 9, the decision on a shared UI module is explicitly deferred. New packages are created directly under `app/src/main/java/com/guidovezzoni/fakestore/ui/{screens,viewmodel,state,intent,effect,util}/`, matching the structure already documented in `docs/guidelines/guidelines-android.md`.

*Alternative considered*: extracting `ProductListItemCard` into a `:ui` module now for future reuse (e.g. favourites screen). Rejected for this story — premature given only one screen exists; revisit when a second screen needs the same card.

### 4. Formatting logic lives in a dedicated `ui/util` file, invoked from the ViewModel
A `ProductListItemFormatter` (or similarly named top-level functions file) under `ui/util/` exposes `formatPrice(price: Double, locale: Locale): String` and `formatRatingScore(score: Double, locale: Locale): String`. Price uses `NumberFormat.getCurrencyInstance(locale)` with its currency explicitly overridden to `Currency.getInstance("USD")` (so the amount is always in USD regardless of device locale, only the symbol/grouping/decimal separator follow locale — e.g. `$109.95` on en-US, `109,95 US$` on es-ES). Rating uses `NumberFormat.getNumberInstance(locale)` with a fixed fraction-digit count. `ProductListViewModel` calls these functions when mapping `Product` → `ProductListItem`, keeping the mapping/formatting logic unit-testable in isolation from Compose and satisfying "composables are purely presentational."

*Alternative considered*: format directly inline in the ViewModel's mapping function. Rejected — the guidelines require testable helper functions in their own file under `ui/util/` even with a single call site, and a dedicated file lets the formatter be unit-tested independently of `ProductListViewModel`'s Hilt/use-case wiring.

### 5. ViewModel obtained via `hiltViewModel()` — new `hilt-navigation-compose` dependency
User-confirmed. `androidx-hilt-navigation-compose` is added purely to unlock the `hiltViewModel()` composable function (no `NavHost` is introduced in this story — `MainActivity` calls `ProductListScreen` directly, which internally calls `hiltViewModel()` to obtain its `ProductListViewModel`). `androidx-lifecycle-viewmodel-compose` is added alongside it as the transitive API `hiltViewModel()` builds on (`collectAsStateWithLifecycle` for observing `uiState`).

*Alternative considered*: manual `ViewModelProvider.Factory` plumbing without `hiltViewModel()`. Rejected — `hilt-navigation-compose` is the standard, minimal-boilerplate mechanism and the project already uses Hilt throughout.

### 6. `LazyColumn` with `key = { it.id }`
User-confirmed. `ProductListScreen`'s `LazyColumn` uses `items(uiState.products, key = { it.id })` so item identity survives recomposition/scroll, per the "List scrolling performance" NFR (stable keys avoid unnecessary recomposition) and to keep `Product.id` (already a stable `Int`) as the natural key.

### 7. `ProductListItem` display model lives in `ui/state/` alongside `ProductListUiState`
`ProductListItem` (`id: Int`, `imageUrl: String`, `title: String`, `formattedPrice: String`, `formattedRatingScore: String`) is a plain immutable data class, one class per file per the Kotlin guideline, placed in `ui/state/` since it is a UI-facing, display-ready shape (not a domain or data-layer concept) referenced by `ProductListUiState.products`.

### 8. Testing patterns established for this story
- **ViewModel test** (`app/src/test/.../ProductListViewModelTest.kt`): MockK-mocked `GetProductsUseCase` returning a controlled `Flow<Result<List<Product>>>`; dispatch `ProductListUiIntent.LoadProducts` via `onIntent()`; assert `uiState.value.products` after `runTest`/`UnconfinedTestDispatcher` collection. A `createViewModel(getProductsUseCase: GetProductsUseCase = ...)` factory consolidates setup per the guideline.
- **Compose UI test** (`app/src/androidTest/.../ProductListScreenTest.kt`): the stateless `ProductListScreen(uiState, onIntent)` overload is exercised directly with hand-built `ProductListUiState` values (no Hilt/ViewModel mocking), asserting card content via `onNodeWithText`/`onNodeWithContentDescription`, and capturing intents into a `mutableListOf`.
- A stateful `ProductListScreen(viewModel: ProductListViewModel = hiltViewModel())` overload delegates to the stateless one, so the Compose UI test targets the stateless signature exclusively — consistent with the "test the composable in isolation" rule.

## Risks / Trade-offs

- **[Risk]** Adding `coil-compose` without `coil-network-okhttp` compiles fine but fails at runtime the first time an image loads (Coil 3's core no longer bundles a network fetcher). → **Mitigation**: both artifacts are added together as a single tracked task in `tasks.md`; the Compose UI test asserts the placeholder/content-description is present, and manual on-device verification (per `guidelines-process.md`) confirms an actual product image renders, not just the placeholder.
- **[Risk]** `NumberFormat.getCurrencyInstance(locale)` defaults to the locale's *own* currency (e.g. EUR for a `es-ES` locale), not USD — naively using it without overriding the `Currency` would silently mis-format prices. → **Mitigation**: `formatPrice` explicitly sets `currency = Currency.getInstance("USD")` on the `NumberFormat` instance before formatting, and the ViewModel test asserts the resulting string for at least two locales (e.g. `en-US` → `$109.95`, `es-ES` → `109,95 US$`).
- **[Risk]** `hiltViewModel()` requires the composable to be called from a `NavBackStackEntry`- or `ComponentActivity`-scoped composition; calling it from a `@Preview` will crash. → **Mitigation**: all `@Preview` functions use the stateless `ProductListScreen(uiState, onIntent)` overload, never the `hiltViewModel()`-backed stateful one — consistent with existing "Composable Previews" guideline requiring previews wrap only stateless composables.
- **[Trade-off]** No loading/error/empty UI is implemented in this story even though `GetProductsUseCase` can emit `Result.failure` — a failure is currently silently swallowed (state simply stays `products = emptyList()`). → Accepted per story scope (1.2.2 covers this); noted here so it isn't mistaken for an oversight during review.
- **[Risk]** Introducing the first ViewModel/Compose UI tests in the repo means there is no existing pattern to copy from — inconsistency risk if a later story diverges. → **Mitigation**: the `createViewModel(...)` factory and stateless-composable-testing pattern are documented explicitly in this design (Decision 8) so subsequent stories (1.2.2 onward) can follow the same shape.

## Migration Plan

No data migration. This is an additive, non-breaking change: `Greeting` is removed and `ProductListScreen` becomes the sole screen `MainActivity` renders, but nothing else in the app currently depends on `Greeting`. Sequencing (captured in `tasks.md`): version catalog + package scaffolding first, then the formatting utility (BDD), then `ProductListViewModel` (BDD), then the Compose UI test + composables (BDD), then strings/DI/`MainActivity` wiring, then final verification (`detektDebug`, `test`, `koverVerify`, on-device check per `guidelines-process.md`).

## Open Questions

None outstanding — image library, placeholder strategy, Spanish resource directory naming, currency behaviour, DI mechanism, and list key strategy were all resolved via user clarification before this design was written.
