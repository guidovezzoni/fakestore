## Context

The Profile screen is the last placeholder screen in the app (`ProfileScreen.kt` currently renders a static `Box { Text(...) }`; `ProfileScreenTest.kt` asserts on that placeholder text and will be fully replaced). `AppDestination.Profile` and its `NavHost` wiring already exist, so no navigation changes are needed. The Products (`ProductListViewModel`/`ProductListScreen`) and Favourites (`FavouritesViewModel`/`FavouritesScreen`) features establish the conventions this screen must follow: MVI contract (`UiState`/`UiIntent`/`UiEffect` sealed interfaces, one file each), a dual-composable screen (stateless for tests/previews, stateful `hiltViewModel()` overload), private per-screen loading/error composables with test tags, and manual (non-`@Inject`) construction of repository implementations wired through Hilt `@Provides` modules.

The FakeStore `GET /users/{id}` endpoint returns a superset of fields, including `password` in plaintext and an `address` object containing geolocation. This is the first endpoint in the app carrying sensitive PII, so the DTO-exclusion pattern (declare only the fields to be consumed; rely on `ignoreUnknownKeys = true`, already set in `NetworkClient.kt`) is the primary security control and must be enforced with a dedicated regression test.

## Goals / Non-Goals

**Goals:**
- Fetch and display the user's full name, email, and a live favourite count on the Profile screen, with loading/error states matching the Products screen's visual pattern.
- Guarantee `password`, `phone`, `address`, and `__v` never enter application memory at any layer.
- Log `profile_screen_viewed` exactly once per screen visit, after the profile has loaded successfully (gated on `Content` state, following the `FavouritesViewModel` pattern).
- Cache the loaded profile in the ViewModel so re-entering the screen does not re-trigger a network call.
- Keep the favourite count reactive and independent of the profile's loading/error state (it is sourced from the existing local `GetFavouriteIdsUseCase`, not the network call).

**Non-Goals:**
- Authentication or user selection — the user ID `8` is hardcoded, per the story's documented assumption.
- Editing profile data — this is a read-only display screen.
- Extracting shared loading/error composables into a `:ui` module — no such module exists yet; this change follows the established per-screen private-composable pattern instead (see Decisions).
- Persisting profile data locally (Room/DataStore) — it is held only in ViewModel memory for the process lifetime, per the story's storage-limitation requirement.

## Decisions

### 1. `UserName` (not `Name`) as the domain value object name
`Name` is a common, collision-prone identifier. `UserName` matches the `UserProfile` naming and avoids ambiguity with any future unrelated "name" concept (e.g. a product name). The DTO mirrors this as `UserNameDto`. This still satisfies the project guideline to preserve the nested `name` object as a distinct value object rather than flattening `firstName`/`lastName` onto `UserProfile`.

### 2. DTO field exclusion as the security boundary, verified by reflection
Rather than deserialising the full API response and discarding sensitive fields downstream, `UserDto` simply never declares `password`, `phone`, `address`, or `__v`. Combined with `ignoreUnknownKeys = true` (already enabled), kotlinx-serialization silently drops them during parsing — they never exist as values in memory. A dedicated `UserMapperTest` (or a standalone DTO test) asserts via reflection that `UserDto::class.java.declaredFields` contains no field named `password`, guarding against a future contributor accidentally adding it back. This is stronger than "don't display it" — the data is structurally impossible to hold.

### 3. Analytics: `TrackScreenViewed` gated on `Content`, following FavouritesViewModel
`ProfileViewModel` follows the `FavouritesViewModel` pattern for analytics: on `TrackScreenViewed`, the ViewModel suspends until `uiState` reaches `Content` (via `uiState.filterIsInstance<Content>().first()`), then logs `profile_screen_viewed` via `AnalyticsClient.logEvent()` exactly once. This deviates from the story's literal wording ("logged regardless of whether the profile data has loaded yet") but keeps the codebase consistent — both `favourites_screen_viewed` and `profile_screen_viewed` use the same content-gated pattern. The event includes no PII parameters.

### 4. Profile caching via `rawProfile` state, mirroring `ProductListViewModel`
`ProductListViewModel` holds `rawProducts = MutableStateFlow<Product?>(null)` and gates re-fetching in its `LoadProducts` handling. `ProfileViewModel` follows the same shape: `rawProfile = MutableStateFlow<UserProfile?>(null)`, combined with the favourite-count flow via `combine()` to derive `ProfileUiState`. On `LoadProfile`, if `rawProfile.value != null` (equivalently, if the current derived state is already `Content`), the use case is not re-invoked — the existing profile is kept and the favourite count continues to update reactively from the already-running `combine()`.

### 5. No shared `:ui` module for loading/error composables
The original story mentions shared composables from `:ui`, but no such module exists — every existing screen (`ProductListScreen`, `FavouritesScreen`) defines its own private `XxxLoadingContent`/`XxxErrorContent` composables with screen-specific test tag constants. Introducing a shared `:ui` module is a larger architectural change out of scope for this story; `ProfileScreen` follows the established per-screen pattern instead. This is documented as a deviation from the original (superseded) story text, consistent with the clarified/updated story at the top of the user story document.

### 6. DI wiring lives in the existing `DataModule`
`UserRepository` → `UserRepositoryImpl` and `GetUserProfileUseCase` are added as `@Provides` functions in the existing `app/di/DataModule.kt`, alongside the `ProductRepository`/`FavouritesRepository` bindings, rather than a new module. This keeps all API/repository/use-case bindings in one place per the existing module's established scope, and does not introduce a new requirement on the `hilt-dependency-injection` capability (no new bootstrap or scoping concern — it is the same `@Provides`/`@Singleton` pattern already governed by that spec's existing requirements).

## Risks / Trade-offs

- **[Risk]** A future contributor adds `password`/`phone`/`address` fields back to `UserDto` for a legitimate-seeming reason (e.g. "we need the phone number now"). → **Mitigation**: the reflection-based `UserMapperTest`/DTO test fails loudly if `password` reappears, forcing a deliberate, reviewed decision rather than an accidental reintroduction.
- **[Risk]** Gating `LoadProfile` on `rawProfile != null` could mask a genuinely stale profile if the underlying user data changes server-side between app sessions within the same process. → **Mitigation**: acceptable per the story's performance section — the demo API is static, and the ViewModel's cache is process-scoped (cleared on ViewModel destruction / process death), not persisted.
- **[Risk]** Diverging from the original story's "shared `:ui` composables" instruction could be seen as an incomplete implementation. → **Mitigation**: explicitly documented in Decision 5 and reflected in the story's own "Assumptions" section (already flagged as an open implementation decision resolved here); consistent with all prior screens' actual code.
- **[Trade-off]** The story's acceptance criteria state `profile_screen_viewed` should fire "regardless of whether the profile data has loaded yet." The implementation gates it on `Content` instead (Decision 3), deviating from the story text but keeping all analytics events consistent with the `FavouritesViewModel` pattern. If the story's wording is revisited, this can be changed to fire unconditionally.

## Migration Plan

Not applicable — this is a net-new screen implementation behind an already-wired, currently-inert placeholder route. No data migration, feature flag, or rollback beyond a standard revert is required.

## Open Questions

None outstanding — all ambiguities from the original story (use case naming, shared vs. per-screen composables, analytics timing, domain value-object naming, DI module placement) were resolved during story clarification and are captured in Decisions above.
