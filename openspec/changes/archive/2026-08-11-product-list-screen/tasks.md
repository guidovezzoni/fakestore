## 1. Version Catalog & Gradle Wiring (Prerequisites)

- [x] 1.1 Add to `gradle/libs.versions.toml`: a `coil` version entry plus `coil-compose` and `coil-network-okhttp` library aliases (`io.coil-kt.coil3:coil-compose`, `io.coil-kt.coil3:coil-network-okhttp`); an `androidx-lifecycle-viewmodel-compose` library alias; and an `androidx-hilt-navigation-compose` library alias
- [x] 1.2 Add the new `coil-compose`, `coil-network-okhttp`, `androidx-lifecycle-viewmodel-compose`, and `androidx-hilt-navigation-compose` dependencies to `app/build.gradle.kts`
- [x] 1.3 Add an `androidx-compose-material-icons-extended` library alias (`androidx.compose.material:material-icons-extended`) to `gradle/libs.versions.toml` and wire it into `app/build.gradle.kts` — required because `Icons.Default.Image` (used as the `AsyncImage` placeholder/error painter in `ProductListItemCard`) is not part of `material-icons-core`

## 2. MVI Package Scaffolding & Display Model (Prerequisites)

- [x] 2.1 Create `app/src/main/java/.../ui/state/ProductListItem.kt` — immutable data class (`id: Int`, `imageUrl: String`, `title: String`, `formattedPrice: String`, `formattedRatingScore: String`)
- [x] 2.2 Create `app/src/main/java/.../ui/state/ProductListUiState.kt` — immutable data class with `products: List<ProductListItem> = emptyList()`
- [x] 2.3 Create `app/src/main/java/.../ui/intent/ProductListUiIntent.kt` — sealed class with `data object LoadProducts`
- [x] 2.4 Create `app/src/main/java/.../ui/effect/ProductListUiEffect.kt` — empty sealed class establishing the one-shot effect structure

## 3. Price and Rating Formatting Utility (BDD)

- [x] 3.1 Write test: GIVEN a price of `109.95` WHEN `formatPrice(109.95, Locale.US)` is called THEN it returns `"$109.95"`, in `ProductListFormatterTest`
- [x] 3.2 Write test: GIVEN a price of `109.95` WHEN `formatPrice(109.95, Locale("es", "ES"))` is called THEN it returns a USD-denominated string using Spanish grouping/decimal conventions (e.g. `"109,95 US$"`), not the Euro symbol, in `ProductListFormatterTest`
- [x] 3.3 Write test: GIVEN a rating score of `4.1` WHEN `formatRatingScore(4.1, Locale.US)` is called THEN it returns `"4.1"`, in `ProductListFormatterTest`
- [x] 3.4 Write test: GIVEN a rating score of `4.1` WHEN `formatRatingScore(4.1, Locale("es", "ES"))` is called THEN it returns `"4,1"`, in `ProductListFormatterTest`
- [x] 3.5 Implement: `app/src/main/java/.../ui/util/ProductListFormatter.kt` — top-level `formatPrice(price: Double, locale: Locale): String` using `NumberFormat.getCurrencyInstance(locale)` with `currency` explicitly overridden to `Currency.getInstance("USD")`, and `formatRatingScore(score: Double, locale: Locale): String` using `NumberFormat.getNumberInstance(locale)` with a fixed fraction-digit count extracted to a named constant

## 4. ProductListViewModel (BDD)

- [x] 4.1 Write test: GIVEN a newly constructed `ProductListViewModel` WHEN no intent has been dispatched THEN `uiState.value.products` equals `emptyList()`, in `ProductListViewModelTest`
- [x] 4.2 Write test: GIVEN a mocked `GetProductsUseCase` whose `invoke()` returns a `Flow` emitting `Result.success` with a list of `Product` domain models WHEN `onIntent(ProductListUiIntent.LoadProducts)` is called THEN `uiState.value.products` contains one mapped `ProductListItem` per input `Product`, in the same order, with each `id` matching its source `Product.id`, in `ProductListViewModelTest`
- [x] 4.3 Write test: GIVEN a mocked `GetProductsUseCase` returning a fixed successful product list WHEN `ProductListUiIntent.LoadProducts` is dispatched twice THEN `uiState.value.products` still equals the mapped list with no duplicated entries, in `ProductListViewModelTest`
- [x] 4.4 Write test: GIVEN a mocked `GetProductsUseCase` returning `Product`s with known price/rating values WHEN `LoadProducts` is dispatched THEN each resulting `ProductListItem.formattedPrice` and `formattedRatingScore` equals the value produced by `ProductListFormatter`'s functions for the same inputs, in `ProductListViewModelTest`
- [x] 4.5 Implement: `app/src/main/java/.../ui/viewmodel/ProductListViewModel.kt` — `@HiltViewModel class ProductListViewModel @Inject constructor(private val getProductsUseCase: GetProductsUseCase) : ViewModel()`, exposing `uiState: StateFlow<ProductListUiState>` and `uiEffect: SharedFlow<ProductListUiEffect>`, with `onIntent(intent: ProductListUiIntent)` handling `LoadProducts` by collecting `getProductsUseCase()` and mapping successful results to `ProductListItem` via `ProductListFormatter`
- [x] 4.6 Add a `createViewModel(getProductsUseCase: GetProductsUseCase = mockk())` factory helper in `ProductListViewModelTest` and refactor tasks 4.1–4.4 to use it instead of inlining the constructor call

## 5. ProductListItemCard Composable (BDD)

- [x] 5.1 Write test: GIVEN a `ProductListItem` with a title, `formattedPrice`, and `formattedRatingScore` WHEN `ProductListItemCard(item)` is composed THEN the title text, formatted price text, and formatted rating score text are each displayed, in `ProductListScreenTest`
- [x] 5.2 Write test: GIVEN a `ProductListItem` WHEN `ProductListItemCard(item)` is composed THEN the product image has a content description equal to the item's title, in `ProductListScreenTest`
- [x] 5.3 Implement: `app/src/main/java/.../ui/screens/ProductListItemCard.kt` — stateless composable rendering a Material 3 `Card` with a Coil 3 `AsyncImage` (content description from `item.title`, `placeholder`/`error` set to a `rememberVectorPainter`-wrapped Material icon), the full title (`Text` with no `maxLines`/`overflow` clipping so it wraps), formatted price, and formatted rating score; extract all dimension literals to named constants
- [x] 5.4 Add a `@Preview` named `PreviewProductListItemCard` wrapped in `FakeStoreTheme`, covering a representative `ProductListItem`

## 6. ProductListScreen Composable (BDD)

- [x] 6.1 Write test: GIVEN a `ProductListUiState` with a non-empty `products` list WHEN the stateless `ProductListScreen(uiState, onIntent)` is composed THEN one `ProductListItemCard` is displayed per item in `uiState.products`, in `ProductListScreenTest`
- [x] 6.2 Write test: GIVEN the stateless `ProductListScreen(uiState, onIntent)` is composed for the first time WHEN composition completes THEN the captured intents list contains `ProductListUiIntent.LoadProducts` exactly once, in `ProductListScreenTest`
- [x] 6.3 Implement: `app/src/main/java/.../ui/screens/ProductListScreen.kt` — stateless `ProductListScreen(uiState: ProductListUiState, onIntent: (ProductListUiIntent) -> Unit, modifier: Modifier = Modifier)` composable with a `LaunchedEffect(Unit) { onIntent(ProductListUiIntent.LoadProducts) }` and a `LazyColumn` using `items(uiState.products, key = { it.id })` rendering `ProductListItemCard` per item
- [x] 6.4 Implement: stateful overload `ProductListScreen(modifier: Modifier = Modifier, viewModel: ProductListViewModel = hiltViewModel())` that collects `viewModel.uiState` via `collectAsStateWithLifecycle()` and delegates to the stateless overload with `onIntent = viewModel::onIntent`
- [x] 6.5 Add a `@Preview` named `PreviewProductListScreen` wrapped in `FakeStoreTheme`, covering a `ProductListUiState` with multiple products

## 7. Strings and Localisation (Integration)

- [x] 7.1 Add English string resources to `app/src/main/res/values/strings.xml`: the product list screen title and the product image content description template
- [x] 7.2 Create `app/src/main/res/values-es/strings.xml` with Spanish translations for every string added in 7.1

## 8. MainActivity Wiring (Integration)

- [x] 8.1 Replace the `Greeting("Android")` call (and the now-unused `Greeting` composable and `GreetingPreview`) in `MainActivity.kt` with the stateful `ProductListScreen()` composable inside the existing `Scaffold`

## 9. Final Verification

- [x] 9.1 Run `./gradlew detektDebug` and resolve any violations (all literals — dimensions, fraction-digit counts — extracted to named constants)
- [x] 9.2 Run `./gradlew test` and confirm `ProductListFormatterTest` and `ProductListViewModelTest` pass
- [x] 9.3 Run `./gradlew connectedDebugAndroidTest` and confirm `ProductListScreenTest` passes on a connected device or emulator
- [x] 9.4 Run `./gradlew koverVerify` (or `koverHtmlReportDebug` for a detailed view) and confirm ≥95% coverage on new `:app` code (`ProductListViewModel`, `ProductListFormatter`)
- [x] 9.5 Install and launch the app on a connected device/emulator (`./gradlew installDebug`, `adb shell am start`); verify via `uiautomator dump` and screenshots that the product list is the initial screen, images load with a visible placeholder-then-content transition, long titles wrap, and the screen renders correctly in both light and dark theme
- [x] 9.6 Switch the device/emulator locale to Spanish and re-verify on-device that the screen title, content descriptions, and price/rating formatting reflect the `values-es` translations and locale-aware formatting
- [x] 9.7 Cross-check the implementation against every Acceptance Criterion and Definition of Done item in `docs/userstories/1.2.1-Product-List-Screen-WIP.md`
