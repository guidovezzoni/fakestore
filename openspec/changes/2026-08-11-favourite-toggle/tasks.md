## 1. Version Catalog & Gradle Wiring (Prerequisites)

- [ ] 1.1 Verify the latest stable `androidx.room` release (Maven Central / `google()`) and add a `room` version entry plus `androidx-room-runtime`, `androidx-room-ktx`, and `androidx-room-compiler` library aliases to `gradle/libs.versions.toml`
- [ ] 1.2 Add `alias(libs.plugins.ksp)` to `data/build.gradle.kts`'s `plugins {}` block, and add `implementation(libs.androidx.room.runtime)`, `implementation(libs.androidx.room.ktx)`, and `ksp(libs.androidx.room.compiler)` to its `dependencies {}` block
- [ ] 1.3 Configure Room schema export in `data/build.gradle.kts` (`ksp { arg("room.schemaLocation", "$projectDir/schemas") }`) so `@Database(exportSchema = true)` has a destination

## 2. Domain Contract (Prerequisite)

- [ ] 2.1 Create `domain/src/main/kotlin/.../domain/repository/FavouritesRepository.kt` — interface with `suspend fun addFavourite(productId: Int)`, `suspend fun removeFavourite(productId: Int)`, `fun getFavouriteIds(): Flow<Set<Int>>`

## 3. Room Persistence Layer (Prerequisites)

- [ ] 3.1 Create `data/src/main/kotlin/.../data/database/FavouriteEntity.kt` — `@Entity(tableName = "favourite_entity") data class FavouriteEntity(@PrimaryKey val productId: Int)`
- [ ] 3.2 Create `data/src/main/kotlin/.../data/database/FavouriteDao.kt` — `@Dao interface FavouriteDao` with `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(entity: FavouriteEntity)`, `@Delete suspend fun delete(entity: FavouriteEntity)`, `@Query("SELECT productId FROM favourite_entity") fun getAllIds(): Flow<List<Int>>`
- [ ] 3.3 Create `data/src/main/kotlin/.../data/database/FavouritesDatabase.kt` — `@Database(entities = [FavouriteEntity::class], version = 1, exportSchema = true) abstract class FavouritesDatabase : RoomDatabase()` exposing `abstract fun favouriteDao(): FavouriteDao`
- [ ] 3.4 Run `./gradlew :data:kspDebugKotlin` (or equivalent compile task) to confirm Room's annotation processor generates `FavouriteDao_Impl`/`FavouritesDatabase_Impl` without errors before proceeding

## 4. FavouritesRepositoryImpl (BDD)

- [ ] 4.1 Write test: GIVEN a `FavouritesRepositoryImpl` backed by a mocked `FavouriteDao` WHEN `addFavourite(productId = 7)` is called THEN `FavouriteDao.insert(FavouriteEntity(productId = 7))` is invoked exactly once, in `FavouritesRepositoryImplTest`
- [ ] 4.2 Write test: GIVEN a `FavouritesRepositoryImpl` backed by a mocked `FavouriteDao` WHEN `removeFavourite(productId = 7)` is called THEN `FavouriteDao.delete(FavouriteEntity(productId = 7))` is invoked exactly once, in `FavouritesRepositoryImplTest`
- [ ] 4.3 Write test: GIVEN a `FavouritesRepositoryImpl` backed by a mocked `FavouriteDao` whose `getAllIds()` emits `listOf(3, 7, 7)` WHEN `getFavouriteIds()` is collected THEN it emits `setOf(3, 7)`, in `FavouritesRepositoryImplTest`
- [ ] 4.4 Implement `data/src/main/kotlin/.../data/repository/FavouritesRepositoryImpl.kt` implementing `FavouritesRepository`, delegating to `FavouriteDao` and mapping `getAllIds()`'s `Flow<List<Int>>` to `Flow<Set<Int>>` via `.map { it.toSet() }`
- [ ] 4.5 Add a `createRepository(favouriteDao: FavouriteDao = mockk())` factory helper in `FavouritesRepositoryImplTest` per the "consolidate test setup" guideline

## 5. ToggleFavouriteUseCase (BDD)

- [ ] 5.1 Write test: GIVEN a `ToggleFavouriteUseCase` backed by a mocked `FavouritesRepository` WHEN `invoke(productId = 7, shouldBeFavourite = true)` is called THEN `FavouritesRepository.addFavourite(7)` is invoked exactly once and the result is `Result.success(Unit)`, in `ToggleFavouriteUseCaseTest`
- [ ] 5.2 Write test: GIVEN a `ToggleFavouriteUseCase` backed by a mocked `FavouritesRepository` WHEN `invoke(productId = 7, shouldBeFavourite = false)` is called THEN `FavouritesRepository.removeFavourite(7)` is invoked exactly once and the result is `Result.success(Unit)`, in `ToggleFavouriteUseCaseTest`
- [ ] 5.3 Write test: GIVEN a `ToggleFavouriteUseCase` backed by a mocked `FavouritesRepository` whose `addFavourite` throws an exception WHEN `invoke(productId = 7, shouldBeFavourite = true)` is called THEN the result is `Result.failure` wrapping that exception and no exception escapes `invoke()`, in `ToggleFavouriteUseCaseTest`
- [ ] 5.4 Implement `domain/src/main/kotlin/.../domain/usecase/ToggleFavouriteUseCase.kt` — `class ToggleFavouriteUseCase(private val repository: FavouritesRepository)` with `suspend operator fun invoke(productId: Int, shouldBeFavourite: Boolean): Result<Unit>` wrapping the add/remove call in `runCatching`

## 6. GetFavouriteIdsUseCase (BDD)

- [ ] 6.1 Write test: GIVEN a `GetFavouriteIdsUseCase` backed by a mocked `FavouritesRepository` whose `getFavouriteIds()` emits `setOf(1, 2)` WHEN `invoke()` is collected THEN it emits `setOf(1, 2)`, in `GetFavouriteIdsUseCaseTest`
- [ ] 6.2 Write test: GIVEN a `GetFavouriteIdsUseCase` backed by a mocked `FavouritesRepository` whose `getFavouriteIds()` emits `emptySet()` WHEN `invoke()` is collected THEN it emits `emptySet()`, in `GetFavouriteIdsUseCaseTest`
- [ ] 6.3 Implement `domain/src/main/kotlin/.../domain/usecase/GetFavouriteIdsUseCase.kt` — `class GetFavouriteIdsUseCase(private val repository: FavouritesRepository)` with `operator fun invoke(): Flow<Set<Int>> = repository.getFavouriteIds()`

## 7. ProductListItem and Mapper Favourite Fields (BDD)

- [ ] 7.1 Write test: GIVEN a product and `isFavourite = true` WHEN `mapToProductListItem(product, locale, isFavourite = true)` is called THEN the resulting `ProductListItem.isFavourite` equals `true`, in `ProductListItemMapperTest`
- [ ] 7.2 Write test: GIVEN a product and `isFavourite = false` WHEN `mapToProductListItem(product, locale, isFavourite = false)` is called THEN the resulting `ProductListItem.isFavourite` equals `false`, in `ProductListItemMapperTest`
- [ ] 7.3 Update the six existing tests in `ProductListItemMapperTest` to pass a fixed `isFavourite` argument so they keep compiling and passing against the new signature
- [ ] 7.4 Extend `app/src/main/java/.../ui/state/ProductListItem.kt` with `isFavourite: Boolean = false`
- [ ] 7.5 Extend `mapToProductListItem` in `app/src/main/java/.../ui/util/ProductListItemMapper.kt` to accept `isFavourite: Boolean` (required, no default) and set it on the returned `ProductListItem`

## 8. ProductListUiIntent and ProductListUiEffect Additions (Prerequisites)

- [ ] 8.1 Add `data class ToggleFavourite(val productId: Int) : ProductListUiIntent` to `app/src/main/java/.../ui/intent/ProductListUiIntent.kt`
- [ ] 8.2 Add `data object ShowFavouriteToggleError : ProductListUiEffect` to `app/src/main/java/.../ui/effect/ProductListUiEffect.kt`
- [ ] 8.3 Create `app/src/main/java/.../ui/state/FavouritesUiState.kt` — sealed interface with `Loading`, `Content(val products: List<ProductListItem>)`, `Error`, mirroring `ProductListUiState`
- [ ] 8.4 Create `app/src/main/java/.../ui/intent/FavouritesUiIntent.kt` — sealed interface with `LoadFavourites` and `ToggleFavourite(val productId: Int)`
- [ ] 8.5 Create `app/src/main/java/.../ui/effect/FavouritesUiEffect.kt` — sealed interface with `ShowFavouriteToggleError`

## 9. ProductListViewModel Combine and Optimistic Toggle (BDD)

- [ ] 9.1 Write test: GIVEN a mocked `GetProductsUseCase` returning products with ids `1` and `2`, and a mocked `GetFavouriteIdsUseCase` emitting `setOf(2)` WHEN `ProductListUiIntent.LoadProducts` is dispatched THEN `uiState.value` is `Content` where the item with `id = 2` has `isFavourite = true` and the item with `id = 1` has `isFavourite = false`, in `ProductListViewModelTest`
- [ ] 9.2 Write test: GIVEN `LoadProducts` has already succeeded WHEN the mocked `GetFavouriteIdsUseCase`'s flow emits a new set including a previously-unfavourited product's id THEN `uiState.value`'s corresponding item's `isFavourite` becomes `true` with no new intent dispatched, in `ProductListViewModelTest`
- [ ] 9.3 Write test: GIVEN `uiState.value` is `Content` containing a product with `id = 7` and `isFavourite = false` WHEN `ProductListUiIntent.ToggleFavourite(productId = 7)` is dispatched, before the underlying write completes THEN `uiState.value`'s item with `id = 7` has `isFavourite = true`, in `ProductListViewModelTest`
- [ ] 9.4 Write test: GIVEN a mocked `ToggleFavouriteUseCase` returning `Result.success(Unit)` and a mocked `AnalyticsClient`, and a product with `id = 7` and `isFavourite = false` WHEN `ToggleFavourite(productId = 7)` is dispatched and the write completes THEN `AnalyticsClient.logEvent()` is invoked exactly once with `name = "favourite_added"` and `params = mapOf("product_id" to 7)`, in `ProductListViewModelTest`
- [ ] 9.5 Write test: GIVEN a mocked `ToggleFavouriteUseCase` returning `Result.success(Unit)`, and a product with `id = 7` and `isFavourite = true` WHEN `ToggleFavourite(productId = 7)` is dispatched and the write completes THEN `AnalyticsClient.logEvent()` is invoked exactly once with `name = "favourite_removed"` and `params = mapOf("product_id" to 7)`, in `ProductListViewModelTest`
- [ ] 9.6 Write test: GIVEN a mocked `ToggleFavouriteUseCase` returning `Result.failure(...)`, a mocked `AnalyticsClient`, and a product with `id = 7` and `isFavourite = false` WHEN `ToggleFavourite(productId = 7)` is dispatched and the write fails THEN `uiState.value`'s item with `id = 7` reverts to `isFavourite = false`, `uiEffect` emits `ProductListUiEffect.ShowFavouriteToggleError`, and `AnalyticsClient.logEvent()` is never invoked with `name = "favourite_added"` or `"favourite_removed"`, in `ProductListViewModelTest`
- [ ] 9.7 Update `ProductListViewModel` constructor and existing tests: add `GetFavouriteIdsUseCase` and `ToggleFavouriteUseCase` parameters; update the `createViewModel(...)` factory helper in `ProductListViewModelTest` with defaults for the new dependencies
- [ ] 9.8 Implement the `rawProducts: MutableStateFlow<List<Product>?>` field and the `init { }`-launched `combine(rawProducts.filterNotNull(), getFavouriteIdsUseCase()) { ... }` collector in `ProductListViewModel`, updating `loadProducts()` to set `rawProducts.value` on success instead of setting `uiState` directly
- [ ] 9.9 Implement `onIntent` handling for `ToggleFavourite`: optimistic `uiState` update, `toggleFavouriteUseCase` call, success analytics logging, and failure revert + `ShowFavouriteToggleError` emission

## 10. ProductListItemCard Favourite Icon (BDD)

- [ ] 10.1 Write test: GIVEN a `ProductListItem` with `isFavourite = true` WHEN `ProductListItemCard(item)` is composed THEN the filled favourite icon is displayed with the localised "Remove from favourites" content description, in `ProductListItemCardTest`
- [ ] 10.2 Write test: GIVEN a `ProductListItem` with `isFavourite = false` WHEN `ProductListItemCard(item)` is composed THEN the outlined favourite icon is displayed with the localised "Add to favourites" content description, in `ProductListItemCardTest`
- [ ] 10.3 Write test: GIVEN a `ProductListItem` with `id = 7` WHEN the user taps the favourite icon on `ProductListItemCard(item, onToggleFavourite)` THEN `onToggleFavourite` is invoked with `7`, in `ProductListItemCardTest`
- [ ] 10.4 Implement the heart `IconButton` in `app/src/main/java/.../ui/screens/ProductListItemCard.kt` — `Icons.Filled.Favorite`/`Icons.Outlined.FavoriteBorder` selected by `item.isFavourite`, `contentDescription = stringResource(if (item.isFavourite) R.string.favourite_remove_content_description else R.string.favourite_add_content_description)`, `onClick = { onToggleFavourite(item.id) }`; add `onToggleFavourite: (Int) -> Unit = {}` parameter
- [ ] 10.5 Update `PreviewProductListItemCard` to add a second preview (or parameterise the existing one) covering both `isFavourite = true` and `isFavourite = false`

## 11. ProductListScreen Snackbar Wiring (BDD)

- [ ] 11.1 Write test: GIVEN `ProductListScreen(uiState, onIntent, uiEffect)` is composed with a `uiEffect` flow that emits `ProductListUiEffect.ShowFavouriteToggleError` WHEN that emission is collected THEN a `Snackbar` displaying the localised `favourite_toggle_error_message` text is shown, in `ProductListScreenTest`
- [ ] 11.2 Write test: GIVEN `uiState` is `Content` containing a product with `id = 7` WHEN the user taps that product card's favourite icon THEN `ProductListUiIntent.ToggleFavourite(productId = 7)` is dispatched to `onIntent`, in `ProductListScreenTest`
- [ ] 11.3 Implement `uiEffect: Flow<ProductListUiEffect> = emptyFlow()` parameter on the stateless `ProductListScreen` composable, a `SnackbarHostState` + `Scaffold(snackbarHost = ...)`, and a `LaunchedEffect` collecting `uiEffect` to show the snackbar on `ShowFavouriteToggleError`
- [ ] 11.4 Wire `ProductListItemCard`'s `onToggleFavourite` parameter in the `LazyColumn` to `onIntent(ProductListUiIntent.ToggleFavourite(item.id))`
- [ ] 11.5 Update the stateful `ProductListScreen()` overload to pass `viewModel.uiEffect` through to the stateless composable

## 12. FavouritesViewModel (BDD)

- [ ] 12.1 Write test: GIVEN a newly constructed `FavouritesViewModel` WHEN no intent has been dispatched THEN `uiState.value` is `FavouritesUiState.Loading`, in `FavouritesViewModelTest`
- [ ] 12.2 Write test: GIVEN a mocked `GetProductsUseCase` returning products with ids `1`, `2`, `3`, and a mocked `GetFavouriteIdsUseCase` emitting `setOf(2)` WHEN `FavouritesUiIntent.LoadFavourites` is dispatched THEN `uiState.value` is `Content` containing exactly one item with `id = 2` and `isFavourite = true`, in `FavouritesViewModelTest`
- [ ] 12.3 Write test: GIVEN a mocked `GetProductsUseCase` returning a non-empty list and a mocked `GetFavouriteIdsUseCase` emitting `emptySet()` WHEN `LoadFavourites` is dispatched THEN `uiState.value` equals `Content(products = emptyList())`, in `FavouritesViewModelTest`
- [ ] 12.4 Write test: GIVEN `LoadFavourites` has already been dispatched and `uiState.value` is `Content` containing a product with `id = 2` WHEN the mocked `GetFavouriteIdsUseCase`'s flow emits a new set no longer containing `2` THEN `uiState.value` updates to no longer contain that product, with no new intent dispatched, in `FavouritesViewModelTest`
- [ ] 12.5 Write test: GIVEN `uiState.value` is `Content` containing a product with `id = 7` WHEN `FavouritesUiIntent.ToggleFavourite(productId = 7)` is dispatched, before the write completes THEN `uiState.value`'s `products` no longer contains a product with `id = 7`, in `FavouritesViewModelTest`
- [ ] 12.6 Write test: GIVEN `uiState.value` is `Content` containing a product with `id = 7`, and a mocked `ToggleFavouriteUseCase` returning `Result.failure(...)` WHEN `ToggleFavourite(productId = 7)` is dispatched and the write completes THEN `uiState.value`'s `products` once again contains the product with `id = 7`, and `uiEffect` emits `FavouritesUiEffect.ShowFavouriteToggleError`, in `FavouritesViewModelTest`
- [ ] 12.7 Write test: GIVEN a mocked `ToggleFavouriteUseCase` returning `Result.success(Unit)` and a mocked `AnalyticsClient` WHEN `ToggleFavourite(productId = 7)` is dispatched and the write succeeds THEN `AnalyticsClient.logEvent()` is invoked exactly once with `name = "favourite_removed"` and `params = mapOf("product_id" to 7)`, in `FavouritesViewModelTest`
- [ ] 12.8 Implement `app/src/main/java/.../ui/viewmodel/FavouritesViewModel.kt` (`@HiltViewModel`, constructor-injected with `GetProductsUseCase`, `GetFavouriteIdsUseCase`, `ToggleFavouriteUseCase`, `AnalyticsClient`), mirroring `ProductListViewModel`'s combine/optimistic-toggle shape but filtering to favourited ids only and always toggling to `shouldBeFavourite = false`
- [ ] 12.9 Add a `createViewModel(...)` factory helper in `FavouritesViewModelTest` with defaults for all constructor dependencies

## 13. FavouritesScreen Composable (BDD)

- [ ] 13.1 Write test: GIVEN `uiState` is `FavouritesUiState.Loading` WHEN `FavouritesScreen(uiState, onIntent)` is composed THEN a loading indicator is displayed and no product cards or empty-state message are visible, in `FavouritesScreenTest`
- [ ] 13.2 Write test: GIVEN `uiState` is `Content` with a non-empty `products` list WHEN `FavouritesScreen(uiState, onIntent)` is composed THEN one card per item is displayed, each keyed by `id`, in `FavouritesScreenTest`
- [ ] 13.3 Write test: GIVEN `uiState` is `Content(products = emptyList())` WHEN `FavouritesScreen(uiState, onIntent)` is composed THEN the localised `favourites_empty_message` text is displayed and no product cards are visible, in `FavouritesScreenTest`
- [ ] 13.4 Write test: GIVEN `uiState` is `Content` with a product `id = 7` displayed WHEN the user taps that product card's heart icon THEN `FavouritesUiIntent.ToggleFavourite(productId = 7)` is dispatched to `onIntent`, in `FavouritesScreenTest`
- [ ] 13.5 Write test: GIVEN the stateful `FavouritesScreen()` overload is composed for the first time WHEN composition completes THEN `FavouritesUiIntent.LoadFavourites` has been dispatched exactly once, in `FavouritesScreenTest`
- [ ] 13.6 Write test: GIVEN `FavouritesScreen(uiState, onIntent, uiEffect)` is composed with a `uiEffect` flow that emits `FavouritesUiEffect.ShowFavouriteToggleError` WHEN that emission is collected THEN a `Snackbar` displaying the localised `favourite_toggle_error_message` text is shown, in `FavouritesScreenTest`
- [ ] 13.7 Implement `app/src/main/java/.../ui/screens/FavouritesScreen.kt` — stateless `FavouritesScreen(uiState: FavouritesUiState, onIntent: (FavouritesUiIntent) -> Unit, uiEffect: Flow<FavouritesUiEffect> = emptyFlow(), modifier: Modifier = Modifier)` reusing `ProductListItemCard` for rendering, a `Scaffold` with `snackbarHost`, an empty-state `Text` (`favourites_empty_message`), and a stateful overload obtaining `FavouritesViewModel` via `hiltViewModel()`
- [ ] 13.8 Add `@Preview` functions covering `Loading`, populated `Content`, and empty `Content` for `FavouritesScreen`, wrapped in `FakeStoreTheme`

## 14. DI Wiring — DatabaseModule (BDD)

- [ ] 14.1 Write test: GIVEN `DatabaseModule.provideFavouritesDatabase` is called with a real application `Context` (Robolectric or instrumented) WHEN the returned `FavouritesDatabase`'s `favouriteDao()` is inspected THEN it is non-null, in `DatabaseModuleTest`
- [ ] 14.2 Write test: GIVEN `DatabaseModule.provideFavouritesRepository` is called with a mocked `FavouriteDao` WHEN the return value's type is inspected THEN it is `FavouritesRepositoryImpl`, in `DatabaseModuleTest`
- [ ] 14.3 Write test: GIVEN `DatabaseModule.provideToggleFavouriteUseCase` is called with a mocked `FavouritesRepository` WHEN the returned `ToggleFavouriteUseCase` is invoked THEN it delegates to the given repository, in `DatabaseModuleTest`
- [ ] 14.4 Write test: GIVEN `DatabaseModule.provideGetFavouriteIdsUseCase` is called with a mocked `FavouritesRepository` WHEN the returned `GetFavouriteIdsUseCase` is invoked and collected THEN it delegates to the given repository's `getFavouriteIds()`, in `DatabaseModuleTest`
- [ ] 14.5 Implement `app/src/main/java/.../di/DatabaseModule.kt` — `@Module @InstallIn(SingletonComponent::class) object DatabaseModule` providing `FavouritesDatabase` (`@Singleton`, via `Room.databaseBuilder(context, ..., DATABASE_NAME).build()` using `@ApplicationContext Context`), `FavouriteDao`, `FavouritesRepository`, `ToggleFavouriteUseCase`, `GetFavouriteIdsUseCase`; extract `DATABASE_NAME` to a private constant

## 15. Strings and Localisation (Integration)

- [ ] 15.1 Add `favourite_add_content_description`, `favourite_remove_content_description`, `favourite_toggle_error_message`, and `favourites_empty_message` to `app/src/main/res/values/strings.xml`
- [ ] 15.2 Add the corresponding Spanish translations to `app/src/main/res/values-es/strings.xml`
- [ ] 15.3 Remove the now-unused `favourites_placeholder` string from both `values/strings.xml` and `values-es/strings.xml` once `FavouritesScreen` no longer references it

## 16. Final Verification

- [ ] 16.1 Run `./gradlew detektDebug` and resolve any violations (all literals — database name, dimensions — extracted to named constants)
- [ ] 16.2 Run `./gradlew test` and confirm all new and updated unit test classes pass: `FavouritesRepositoryImplTest`, `ToggleFavouriteUseCaseTest`, `GetFavouriteIdsUseCaseTest`, `ProductListItemMapperTest`, `ProductListViewModelTest`, `FavouritesViewModelTest`, `DatabaseModuleTest`
- [ ] 16.3 Run `./gradlew connectedDebugAndroidTest` and confirm `ProductListItemCardTest`, `ProductListScreenTest`, and `FavouritesScreenTest` pass on a connected device or emulator
- [ ] 16.4 Run `./gradlew koverHtmlReportDebug` (or `koverVerify`) and confirm ≥95% coverage on new/changed `:domain`, `:data`, and `:app` code
- [ ] 16.5 Install and launch the app (`./gradlew installDebug`, `adb shell am start`); using `uiautomator dump` and screenshots, verify: tapping a heart icon on the Products screen toggles it immediately, the same product's icon state matches on the Favourites screen, removing a favourite from the Favourites screen removes it from the displayed list, and the empty-state message appears when no favourites remain
- [ ] 16.6 Force a toggle failure (e.g. temporarily throw from `FavouriteDao` or disable the DB) and verify on-device that the icon reverts and a snackbar with the error message appears
- [ ] 16.7 Force-stop and relaunch the app; verify on-device that previously-favourited products still show filled heart icons on both screens (persistence survives restart)
- [ ] 16.8 Switch the device/emulator locale to Spanish and re-verify on-device that the new content descriptions, error snackbar, and empty-state message reflect the `values-es` translations
- [ ] 16.9 Cross-check the implementation against every Acceptance Criterion and Definition of Done item in `docs/userstories/2.1.1-Favourite-Toggle-WIP.md`
