## Why

The Profile screen is currently a placeholder (`Box { Text(...) }`) with no real data. Users need to see their account details (name, email) and a live count of favourited products, following the same MVI, loading/error, and analytics conventions already established by the Products and Favourites screens. This closes out Epic 3.1 (User Profile) for the demo app.

## What Changes

- Add a `GET /users/{id}` endpoint to `ApiService`, called with a hardcoded user ID of `8` (no auth/user-selection in scope).
- Add `UserDto` and `UserNameDto` (`:data`) matching only the fields consumed (`id`, `email`, `username`, nested `name.firstname`/`name.lastname`). The `password`, `phone`, `address`, and `__v` fields from the API response are **never** declared in any DTO, so they are silently dropped during deserialisation (relying on the already-enabled `ignoreUnknownKeys = true`) — this is a security/GDPR-critical exclusion, not an incidental omission.
- Add `UserProfile` and `UserName` value objects (`:domain`), a `UserRepository` interface (`:domain`) plus `UserRepositoryImpl` (`:data`), and `GetUserProfileUseCase` (`:domain`) exposing `Flow<Result<UserProfile>>`, mirroring the existing `ProductRepository`/`GetProductsUseCase` pattern.
- Add `ProfileUiState` (`Loading`/`Content(fullName, email, favouriteCount)`/`Error`), `ProfileUiIntent` (`LoadProfile`, `RetryClicked`, `TrackScreenViewed`), `ProfileUiEffect` (empty/placeholder), and `ProfileViewModel` (`:app`), which combines the profile fetch with the existing `GetFavouriteIdsUseCase` reactive count, caches the loaded profile (no re-fetch on re-entry), and logs `profile_screen_viewed` on `TrackScreenViewed` after reaching `Content` state (following the `FavouritesViewModel` pattern, gated via `filterIsInstance<Content>().first()`).
- Replace the placeholder `ProfileScreen` composable with a full implementation: centred loading indicator, centred error message + retry button (matching the Products screen pattern), and a content view showing full name, email, and favourite count, all with test tags and accessible semantics.
- Wire `UserRepository`/`UserRepositoryImpl`/`GetUserProfileUseCase` into the existing `DataModule`.
- Add new string resources (screen title, favourite count label) to `values/strings.xml` and `values-es/strings.xml`, reusing `product_list_error_message`/`product_list_retry_button` for the error state.

## Capabilities

### New Capabilities
- `user-profile-data`: Domain and data-layer requirements for fetching a user profile from the FakeStore API (`:core`, `:domain`, `:data`) — DTOs, mapper, repository, use case, and the mandatory exclusion of `password`/`phone`/`address`/`__v` fields.
- `profile-screen`: UI-layer requirements for the Profile screen (`:app`) — MVI contract, reactive favourite count, loading/error/content rendering, caching behaviour, analytics, and localisation.

### Modified Capabilities
- None. `hilt-dependency-injection` and existing DI modules follow their established pattern without a requirements-level change; wiring `UserRepository` into `DataModule` is an implementation detail of the `user-profile-data` capability, not a new DI requirement.

## Impact

- **`:domain`**: new files `model/UserProfile.kt`, `model/UserName.kt`, `repository/UserRepository.kt`, `usecase/GetUserProfileUseCase.kt`.
- **`:data`**: new files `model/UserDto.kt`, `model/UserNameDto.kt`, `mapper/UserMapper.kt`, `repository/UserRepositoryImpl.kt`; modified `network/ApiService.kt`.
- **`:app`**: new files `ui/state/ProfileUiState.kt`, `ui/intent/ProfileUiIntent.kt`, `ui/effect/ProfileUiEffect.kt`, `ui/viewmodel/ProfileViewModel.kt`; modified `ui/screens/ProfileScreen.kt`, `di/DataModule.kt`, `res/values/strings.xml`, `res/values-es/strings.xml`.
- **Tests**: new `ProfileViewModelTest`, `GetUserProfileUseCaseTest`, `UserRepositoryImplTest`, `UserMapperTest` (unit, `:app`/`:domain`/`:data` as applicable), replaced `ProfileScreenTest` (Compose UI, androidTest); extended `HiltDependencyGraphTest`.
- **No breaking changes** — the Profile screen and its route already exist; this replaces an inert placeholder with a working implementation.
