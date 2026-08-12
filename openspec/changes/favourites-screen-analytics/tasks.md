## 1. FavouritesUiIntent Prerequisite

- [ ] 1.1 Add `data object TrackScreenViewed : FavouritesUiIntent` to `app/src/main/java/com/guidovezzoni/fakestore/ui/intent/FavouritesUiIntent.kt`, alongside the existing `LoadFavourites` and `ToggleFavourite`

## 2. FavouritesViewModel Analytics (BDD)

- [ ] 2.1 Write test: `GIVEN Content with 3 favourite products WHEN TrackScreenViewed is dispatched THEN favourites_screen_viewed is logged with favourite_count = 3` in `FavouritesViewModelTest`
- [ ] 2.2 Write test: `GIVEN Content with empty product list WHEN TrackScreenViewed is dispatched THEN favourites_screen_viewed is logged with favourite_count = 0` in `FavouritesViewModelTest`
- [ ] 2.3 Write test: `GIVEN Loading state WHEN TrackScreenViewed is dispatched THEN no analytics event is logged` in `FavouritesViewModelTest`
- [ ] 2.4 Write test: `GIVEN Error state WHEN TrackScreenViewed is dispatched THEN no analytics event is logged` in `FavouritesViewModelTest`
- [ ] 2.5 Write test: `GIVEN Content WHEN TrackScreenViewed is dispatched twice THEN favourites_screen_viewed is logged twice` in `FavouritesViewModelTest`
- [ ] 2.6 Implement `trackScreenViewed()` in `FavouritesViewModel` — launches `viewModelScope.launch { uiState.filterIsInstance<FavouritesUiState.Content>().first() }`, then logs `EVENT_FAVOURITES_SCREEN_VIEWED` with `params = mapOf(PARAM_FAVOURITE_COUNT to content.products.size)` using the captured emission (not a fresh `_uiState.value` read)
- [ ] 2.7 Add `is FavouritesUiIntent.TrackScreenViewed -> trackScreenViewed()` to `FavouritesViewModel.onIntent()`'s `when` block
- [ ] 2.8 Add `EVENT_FAVOURITES_SCREEN_VIEWED = "favourites_screen_viewed"` and `PARAM_FAVOURITE_COUNT = "favourite_count"` constants to `FavouritesViewModel`'s existing `private companion object`, alongside `EVENT_FAVOURITE_REMOVED`/`PARAM_PRODUCT_ID`

## 3. FavouritesScreen Dispatch and Error-State Coverage (BDD)

- [ ] 3.1 Write test: `givenFavouritesScreen_whenComposed_thenTrackScreenViewedIntentIsFiredExactlyOnce` in `FavouritesScreenTest` — asserts `capturedIntents.count { it == FavouritesUiIntent.TrackScreenViewed } == 1`, mirroring the existing `givenFavouritesScreen_whenComposed_thenLoadFavouritesIntentIsFiredExactlyOnce` test
- [ ] 3.2 Write test: `givenErrorState_whenFavouritesScreenIsComposed_thenErrorMessageIsDisplayed` in `FavouritesScreenTest` — composes `FavouritesScreen(uiState = FavouritesUiState.Error, onIntent = {})` and asserts the localised `product_list_error_message` text is displayed (closes the pre-existing coverage gap; `FavouritesErrorContent` is already implemented, so this task adds test coverage only, with no production code change expected)
- [ ] 3.3 Implement: add `currentOnIntent(FavouritesUiIntent.TrackScreenViewed)` to the existing `LaunchedEffect(Unit)` block in `FavouritesScreen.kt`, immediately after the existing `currentOnIntent(FavouritesUiIntent.LoadFavourites)` call

## 4. Empty-State Message Copy (BDD)

- [ ] 4.1 Write test: `givenEmptyContentState_whenFavouritesScreenIsComposed_thenEmptyMessageContainsInstructionalText` in `FavouritesScreenTest` — composes `FavouritesScreen(uiState = FavouritesUiState.Content(products = emptyList()), onIntent = {})` and asserts the displayed text equals "No favourites yet. Tap the heart on a product to save it."
- [ ] 4.2 Implement: update `favourites_empty_message` in `app/src/main/res/values/strings.xml` from "No favourites yet" to "No favourites yet. Tap the heart on a product to save it."
- [ ] 4.3 Implement: update `favourites_empty_message` in `app/src/main/res/values-es/strings.xml` from "Sin favoritos aún" to "Sin favoritos aún. Toca el corazón en un producto para guardarlo."

## 5. Final Verification

- [ ] 5.1 Run `./gradlew detektDebug` and resolve any violations (new literals — event name, param key — extracted to named constants per task 2.8)
- [ ] 5.2 Run `./gradlew test` and confirm `FavouritesViewModelTest` passes in full, including all five new `TrackScreenViewed` scenarios, with no regressions to existing `LoadFavourites`/`ToggleFavourite` tests
- [ ] 5.3 Run `./gradlew connectedDebugAndroidTest` and confirm `FavouritesScreenTest` passes in full, including the new `TrackScreenViewed`, empty-message, and `Error`-state tests
- [ ] 5.4 Run `./gradlew koverHtmlReportDebug` (or `koverVerify`) and confirm ≥95% coverage is maintained on `FavouritesViewModel`
- [ ] 5.5 Install and launch the app (`./gradlew installDebug`, `adb shell am start`); using `uiautomator dump`, verify the Favourites screen shows favourited products, the empty state displays the updated instructional message, removing a favourite updates the list, and navigating to the screen via the bottom bar works correctly
- [ ] 5.6 Switch the device/emulator locale to Spanish and re-verify on-device that the updated `favourites_empty_message` translation is displayed correctly
- [ ] 5.7 Cross-check the implementation against every Acceptance Criterion and Definition of Done item in `docs/userstories/2.2.1-Favourites-Screen-WIP.md`
