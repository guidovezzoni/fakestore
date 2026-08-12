## 1. Prerequisites: Domain and Data Models

- [x] 1.1 Create `UserName` data class (`:domain` `model/UserName.kt`) with `firstName: String`, `lastName: String`
- [x] 1.2 Create `UserProfile` data class (`:domain` `model/UserProfile.kt`) with `id: Int`, `userName: String`, `name: UserName`, `email: String`
- [x] 1.3 Create `UserRepository` interface (`:domain` `repository/UserRepository.kt`) with `suspend fun getUserProfile(id: Int): UserProfile`
- [x] 1.4 Create `UserNameDto` `@Serializable` data class (`:data` `model/UserNameDto.kt`) with `firstname: String`, `lastname: String`
- [x] 1.5 Create `UserDto` `@Serializable` data class (`:data` `model/UserDto.kt`) with `id: Int`, `email: String`, `username: String`, `name: UserNameDto` — MUST NOT declare `password`, `phone`, `address`, or `__v`
- [x] 1.6 Add `suspend fun getUser(@Path("id") id: Int): UserDto` to `ApiService` (`GET users/{id}`)

## 2. UserMapper (BDD)

- [x] 2.1 Write test: GIVEN a `UserDto` with lowercase name fields WHEN mapped THEN the domain `UserName` fields preserve the original casing exactly, in `UserMapperTest`
- [x] 2.2 Write test: GIVEN a `UserDto` with all fields populated WHEN mapped THEN all domain model fields (`id`, `userName`, `name.firstName`, `name.lastName`, `email`) are correctly set, in `UserMapperTest`
- [x] 2.3 Write test: GIVEN `UserDto`'s declared fields WHEN inspected via reflection THEN no field named `password`, `phone`, `address`, or `__v` exists, in `UserMapperTest`
- [x] 2.4 Implement `UserMapper` (`internal object` in `:data` `mapper/UserMapper.kt`) mapping `UserDto`/`UserNameDto` to `UserProfile`/`UserName` with no casing changes, to make all `UserMapperTest` cases pass

## 3. UserRepositoryImpl (BDD)

- [x] 3.1 Write test: GIVEN the API returns a valid `UserDto` WHEN `UserRepositoryImpl.getUserProfile(id)` is called THEN it returns a correctly mapped `UserProfile` domain model, in `UserRepositoryImplTest`
- [x] 3.2 Write test: GIVEN `ApiService.getUser(id)` throws an `IOException` WHEN `UserRepositoryImpl.getUserProfile(id)` is called THEN the exception propagates unchanged, in `UserRepositoryImplTest`
- [x] 3.3 Implement `UserRepositoryImpl` (`:data` `repository/UserRepositoryImpl.kt`), calling `ApiService.getUser` and applying `UserMapper`, to make `UserRepositoryImplTest` pass

## 4. GetUserProfileUseCase (BDD)

- [x] 4.1 Write test: GIVEN the repository returns a valid `UserProfile` WHEN the use case is invoked THEN it emits `Result.success` with the profile and the `Flow` completes, in `GetUserProfileUseCaseTest`
- [x] 4.2 Write test: GIVEN the repository throws an exception WHEN the use case is invoked THEN it emits `Result.failure` with the exception and the `Flow` completes, in `GetUserProfileUseCaseTest`
- [x] 4.3 Implement `GetUserProfileUseCase` (`:domain` `usecase/GetUserProfileUseCase.kt`) with `operator fun invoke(id: Int): Flow<Result<UserProfile>>`, to make `GetUserProfileUseCaseTest` pass

## 5. Prerequisites: Profile MVI Contract

- [x] 5.1 Create `ProfileUiState` sealed interface (`:app` `ui/state/ProfileUiState.kt`) with `Loading`, `Content(fullName: String, email: String, favouriteCount: Int)`, `Error`
- [x] 5.2 Create `ProfileUiIntent` sealed interface (`:app` `ui/intent/ProfileUiIntent.kt`) with `LoadProfile`, `RetryClicked`, `TrackScreenViewed`
- [x] 5.3 Create `ProfileUiEffect` sealed interface (`:app` `ui/effect/ProfileUiEffect.kt`), empty/placeholder for future one-shot effects

## 6. ProfileViewModel (BDD)

- [x] 6.1 Write test: GIVEN a fresh `ProfileViewModel` WHEN `LoadProfile` is dispatched THEN state transitions `Loading` then `Content` with correct `fullName`, `email`, and `favouriteCount`, in `ProfileViewModelTest`
- [x] 6.2 Write test: GIVEN a fresh `ProfileViewModel` WHEN the profile API call fails THEN state transitions to `Error`, in `ProfileViewModelTest`
- [x] 6.3 Write test: GIVEN `Error` state WHEN `RetryClicked` is dispatched THEN state transitions to `Loading` and re-invokes the use case, in `ProfileViewModelTest`
- [x] 6.4 Write test: GIVEN `Content` state WHEN favourites change on another screen (`GetFavouriteIdsUseCase` emits a new set) THEN `favouriteCount` updates reactively without re-fetching the profile, in `ProfileViewModelTest`
- [x] 6.5 Write test: GIVEN `Content` state (profile already loaded) WHEN `LoadProfile` is dispatched again THEN `GetUserProfileUseCase` is not invoked a second time and state remains `Content`, in `ProfileViewModelTest`
- [x] 6.6 Write test: GIVEN a fresh `ProfileViewModel` WHEN `TrackScreenViewed` is dispatched and profile loads successfully THEN `profile_screen_viewed` is logged via `AnalyticsClient.logEvent()` exactly once after reaching `Content`, in `ProfileViewModelTest`
- [x] 6.7 Write test: GIVEN profile loading fails (state is `Error`) WHEN `TrackScreenViewed` was dispatched THEN the analytics event is NOT logged (it waits for `Content` which never arrives), in `ProfileViewModelTest`
- [x] 6.8 Write test: GIVEN `TrackScreenViewed` is dispatched WHEN `AnalyticsClient.logEvent()` is invoked THEN the call parameters contain no PII (name or email), in `ProfileViewModelTest`
- [x] 6.9 Implement `ProfileViewModel` (`:app` `ui/viewmodel/ProfileViewModel.kt`): `rawProfile = MutableStateFlow<UserProfile?>(null)` combined with `GetFavouriteIdsUseCase()` via `combine()` to derive `uiState`; `onIntent()` handling for `LoadProfile` (skip re-fetch if `rawProfile.value != null`), `RetryClicked` (always re-fetch), and `TrackScreenViewed` (wait for `Content` via `uiState.filterIsInstance<Content>().first()` then log, no PII params — following the FavouritesViewModel pattern) — to make all `ProfileViewModelTest` cases pass

## 7. ProfileScreen Composable (BDD)

- [x] 7.1 Write test: GIVEN `ProfileUiState.Loading` WHEN the screen renders THEN the loading indicator is visible and no content/error UI is shown, in `ProfileScreenTest` — RED confirmed (compile check)
- [x] 7.2 Write test: GIVEN `ProfileUiState.Content` with a name, email, and count WHEN the screen renders THEN all three values are displayed, in `ProfileScreenTest` — RED confirmed (compile check)
- [x] 7.3 Write test: GIVEN `ProfileUiState.Error` WHEN the screen renders THEN the error message and retry button are visible, in `ProfileScreenTest` — RED confirmed (compile check)
- [x] 7.4 Write test: GIVEN `ProfileUiState.Error` WHEN the retry button is tapped THEN `ProfileUiIntent.RetryClicked` is emitted to `onIntent`, in `ProfileScreenTest` — RED confirmed (compile check)
- [x] 7.5 Write test: GIVEN the stateful `ProfileScreen()` overload is composed for the first time WHEN composition completes THEN `LoadProfile` and `TrackScreenViewed` have each been dispatched exactly once, in `ProfileScreenTest` — RED confirmed (compile check)
- [x] 7.6 Write test: GIVEN `ProfileUiState.Error` WHEN the retry button is inspected by an accessibility service THEN it is focusable, has a non-empty accessible label, and is operable, in `ProfileScreenTest` — RED confirmed (compile check)
- [x] 7.7 Implement `ProfileScreen` (`:app` `ui/screens/ProfileScreen.kt`): replace the placeholder with stateless `ProfileScreen(uiState, onIntent, modifier)` plus a stateful `hiltViewModel()` overload, private loading/error/content composables with test tags, and Composable previews covering `Loading`, `Content`, and `Error` — to make all `ProfileScreenTest` cases pass — GREEN confirmed (compile check)

## 8. String Resources

- [x] 8.1 Add `profile_screen_title` and `profile_favourite_count_label` string resources to `app/src/main/res/values/strings.xml`
- [x] 8.2 Add corresponding Spanish translations for `profile_screen_title` and `profile_favourite_count_label` to `app/src/main/res/values-es/strings.xml`
- [x] 8.3 Wire `ProfileScreen`'s error and retry UI to the existing `product_list_error_message` and `product_list_retry_button` resources (no new duplicate strings)

## 9. Dependency Injection Wiring

- [x] 9.1 Register `@Provides` bindings for `UserRepository` → `UserRepositoryImpl` and `GetUserProfileUseCase` in the existing `app/di/DataModule.kt`, alongside the existing product/favourites bindings
- [x] 9.2 Extend `HiltDependencyGraphTest` to assert `UserRepository` resolves from the Hilt graph

## 10. Final Verification

- [x] 10.1 Run `./gradlew check` (unit tests + detekt + lint) and confirm it passes
- [x] 10.2 Run `./gradlew koverHtmlReportDebug` and confirm coverage is >= 95% for all new code
- [x] 10.3 Run a repo-wide search confirming `password`, `phone`, `address`, and `__v` do not appear as declared properties in any DTO or domain model
- [x] 10.4 Confirm no PII (name, email) appears in any `AnalyticsClient.logEvent()` call, log statement, or crash-reporting call added by this change
- [x] 10.5 Run `./gradlew connectedDebugAndroidTest` on a connected device and confirm all `ProfileScreenTest` and `HiltDependencyGraphTest` cases pass
- [x] 10.6 On-device manual verification: Profile screen displays name/email/favourite count correctly for user id 8; favourite count updates live when toggling a product's favourite status on the Products screen; simulating a network failure shows the error state, and tapping retry recovers to `Content` once the network is available again
