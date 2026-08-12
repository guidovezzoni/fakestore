## ADDED Requirements

### Requirement: ProfileUiState models loading, content, and error
`:app` SHALL define `ProfileUiState` as a sealed interface with `Loading`, `Content(val fullName: String, val email: String, val favouriteCount: Int)`, and `Error` states, representing the full state of the Profile screen at any point in time.

#### Scenario: Loading, Content, and Error are distinct sealed entries
- **WHEN** `ProfileUiState.Loading`, `ProfileUiState.Content(fullName = "William Hopkins", email = "william@gmail.com", favouriteCount = 3)`, and `ProfileUiState.Error` are referenced
- **THEN** all three are members of the `ProfileUiState` sealed interface, usable in an exhaustive `when` block when rendering the screen

### Requirement: ProfileUiIntent models loading, retrying, and tracking a screen view
`:app` SHALL define `ProfileUiIntent` as a sealed interface with `LoadProfile` (triggered on first composition), `RetryClicked` (triggered by tapping the error state's retry button), and `TrackScreenViewed` (triggered on first composition, signalling `ProfileViewModel` to log the `profile_screen_viewed` analytics event).

#### Scenario: LoadProfile, RetryClicked, and TrackScreenViewed are distinct sealed entries
- **WHEN** `ProfileUiIntent.LoadProfile`, `ProfileUiIntent.RetryClicked`, and `ProfileUiIntent.TrackScreenViewed` are referenced
- **THEN** all three are members of the `ProfileUiIntent` sealed interface, usable in an exhaustive `when` block over `ProfileViewModel.onIntent()`

### Requirement: ProfileViewModel combines profile fetch and reactive favourite count
`:app` SHALL define `ProfileViewModel`, which holds `rawProfile: MutableStateFlow<UserProfile?>` (initially `null`) and combines it with `GetFavouriteIdsUseCase()`'s emitted favourite ID set via `combine()` to derive `uiState: StateFlow<ProfileUiState>`. While `rawProfile` is `null`, `uiState` SHALL be `Loading` (unless an error has been recorded, in which case `Error`); once `rawProfile` holds a `UserProfile`, `uiState` SHALL be `Content` with `favouriteCount` equal to the current favourite ID set's size, recomputed on every emission from `GetFavouriteIdsUseCase()` without re-fetching the profile.

#### Scenario: Favourite count updates without a profile re-fetch
- **GIVEN** `uiState.value` is `ProfileUiState.Content` with `favouriteCount = 2`
- **WHEN** `GetFavouriteIdsUseCase()`'s underlying flow emits a set with 3 favourite IDs (a product was favourited on another screen)
- **THEN** `uiState.value` becomes `ProfileUiState.Content` with the same `fullName`/`email` and `favouriteCount = 3`, with no new call to `GetUserProfileUseCase`

#### Scenario: Favourite count updates while a profile reload is in flight
- **GIVEN** `uiState.value` is `ProfileUiState.Loading` because `LoadProfile` has been dispatched but the use case has not yet emitted
- **WHEN** `GetFavouriteIdsUseCase()`'s underlying flow emits a favourite ID set
- **THEN** the favourite count is reflected once `rawProfile` becomes non-null and `uiState` transitions to `Content`, and the reactive favourite-count subscription is not blocked by the in-flight profile fetch

### Requirement: LoadProfile fetches once and does not re-fetch an already-loaded profile
On `ProfileUiIntent.LoadProfile`, `ProfileViewModel` SHALL invoke `GetUserProfileUseCase(id = 8)` only if `rawProfile.value` is currently `null` (i.e. no profile has been successfully loaded yet). On success, `rawProfile` SHALL be set to the fetched `UserProfile`, driving `uiState` to `Content`. On failure, `uiState` SHALL become `Error` and `rawProfile` SHALL remain `null`. If `rawProfile.value` is already non-null when `LoadProfile` is dispatched, no new use-case invocation SHALL occur.

#### Scenario: First LoadProfile fetches and populates Content
- **GIVEN** a freshly constructed `ProfileViewModel` with `rawProfile.value == null`
- **WHEN** `ProfileUiIntent.LoadProfile` is dispatched and `GetUserProfileUseCase` emits `Result.success` with a `UserProfile`
- **THEN** `uiState` transitions from `Loading` to `Content` with `fullName` derived from the profile's `UserName` (`firstName` + `" "` + `lastName`) and `email` equal to the profile's `email`

#### Scenario: LoadProfile fails and surfaces Error
- **GIVEN** a freshly constructed `ProfileViewModel`
- **WHEN** `ProfileUiIntent.LoadProfile` is dispatched and `GetUserProfileUseCase` emits `Result.failure`
- **THEN** `uiState` transitions to `ProfileUiState.Error`

#### Scenario: Re-dispatching LoadProfile after Content is reached does not re-invoke the use case
- **GIVEN** `uiState.value` is `ProfileUiState.Content` (the profile has already been successfully loaded once)
- **WHEN** `ProfileUiIntent.LoadProfile` is dispatched again
- **THEN** `GetUserProfileUseCase` is not invoked a second time, and `uiState` remains `Content` with the same profile data

### Requirement: RetryClicked re-attempts the profile fetch from Error
On `ProfileUiIntent.RetryClicked`, `ProfileViewModel` SHALL re-invoke `GetUserProfileUseCase(id = 8)` regardless of the current `rawProfile` value, transitioning `uiState` to `Loading` and then to `Content` or `Error` based on the outcome.

#### Scenario: Retry after an error succeeds
- **GIVEN** `uiState.value` is `ProfileUiState.Error`
- **WHEN** `ProfileUiIntent.RetryClicked` is dispatched and `GetUserProfileUseCase` emits `Result.success` with a `UserProfile`
- **THEN** `uiState` transitions to `ProfileUiState.Content` with the profile's data

### Requirement: profile_screen_viewed is logged after Content is reached on TrackScreenViewed
On `ProfileUiIntent.TrackScreenViewed`, `ProfileViewModel` SHALL suspend until `uiState` reaches `ProfileUiState.Content` (via `uiState.filterIsInstance<Content>().first()`), then log `profile_screen_viewed` via `AnalyticsClient.logEvent()` exactly once. This follows the `FavouritesViewModel` pattern for analytics timing. The event SHALL NOT include any PII (name or email) as a parameter.

#### Scenario: TrackScreenViewed logs after Content is reached
- **GIVEN** `ProfileUiIntent.TrackScreenViewed` is dispatched while `uiState.value` is `ProfileUiState.Loading`
- **WHEN** the profile loads successfully and `uiState` transitions to `ProfileUiState.Content`
- **THEN** `AnalyticsClient.logEvent()` is invoked exactly once with `name = "profile_screen_viewed"`

#### Scenario: TrackScreenViewed does not log if Content is never reached
- **GIVEN** `ProfileUiIntent.TrackScreenViewed` is dispatched
- **WHEN** the profile fetch fails and `uiState` transitions to `ProfileUiState.Error` without ever reaching `Content`
- **THEN** `AnalyticsClient.logEvent()` is NOT invoked

#### Scenario: The logged event contains no PII
- **WHEN** `ProfileUiIntent.TrackScreenViewed` is dispatched and `AnalyticsClient.logEvent()` is invoked after reaching `Content`
- **THEN** the call's parameters do not contain the user's name or email in any form

### Requirement: ProfileScreen renders state-specific UI with loading indicator, content, and retry
`:app` SHALL define a stateless `ProfileScreen(uiState: ProfileUiState, onIntent: (ProfileUiIntent) -> Unit, modifier: Modifier = Modifier)` composable. It SHALL render a centred loading indicator (tagged for testing) for `Loading`; for `Content`, the full name, email, and a localised favourite-count label displaying `favouriteCount`, each with an accessible text semantics and a test tag; and for `Error`, a centred, localised error message (reusing `product_list_error_message`) plus a focusable, localised retry button (reusing `product_list_retry_button`) that dispatches `RetryClicked` when tapped. A stateful overload SHALL obtain `ProfileViewModel` via `hiltViewModel()` and dispatch both `LoadProfile` and `TrackScreenViewed` from the same `LaunchedEffect(Unit)` on first composition.

#### Scenario: Loading state shows only the loading indicator
- **GIVEN** `uiState` is `ProfileUiState.Loading`
- **WHEN** `ProfileScreen(uiState, onIntent)` is composed
- **THEN** a loading indicator is displayed and no name, email, favourite count, or error/retry UI is visible

#### Scenario: Content state shows name, email, and favourite count
- **GIVEN** `uiState` is `ProfileUiState.Content(fullName = "William Hopkins", email = "william@gmail.com", favouriteCount = 3)`
- **WHEN** `ProfileScreen(uiState, onIntent)` is composed
- **THEN** the text "William Hopkins", the text "william@gmail.com", and a favourite-count display reflecting `3` are all displayed

#### Scenario: Error state shows a centred error message and a retry button
- **GIVEN** `uiState` is `ProfileUiState.Error`
- **WHEN** `ProfileScreen(uiState, onIntent)` is composed
- **THEN** the localised error message and a retry button are displayed, and no name, email, favourite count, or loading indicator are visible

#### Scenario: Tapping retry dispatches RetryClicked
- **GIVEN** `uiState` is `ProfileUiState.Error`
- **WHEN** the user taps the retry button
- **THEN** `ProfileUiIntent.RetryClicked` is dispatched to `onIntent`

#### Scenario: LoadProfile and TrackScreenViewed both fire on first composition
- **GIVEN** the stateful `ProfileScreen()` overload is composed for the first time
- **WHEN** composition completes
- **THEN** `ProfileUiIntent.LoadProfile` and `ProfileUiIntent.TrackScreenViewed` have each been dispatched to the underlying `ProfileViewModel.onIntent()` exactly once

#### Scenario: Retry button is accessible
- **GIVEN** `uiState` is `ProfileUiState.Error`
- **WHEN** the retry button is inspected by an accessibility service
- **THEN** it is focusable, has a non-empty accessible label, and is operable (clickable) via accessibility services

### Requirement: Profile-related strings are localised in English and Spanish
`app/src/main/res/values/strings.xml` SHALL define string resources for the Profile screen title and the favourite-count label as English (base) string resources, each with a corresponding translated entry in `app/src/main/res/values-es/strings.xml`. The error message and retry button label SHALL reuse the existing `product_list_error_message` and `product_list_retry_button` resources rather than duplicating identical text under new keys. No Profile-screen string SHALL appear as a hardcoded literal in source code.

#### Scenario: Every new Profile string resource has a Spanish translation
- **WHEN** `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml` are compared
- **THEN** the new Profile screen title and favourite-count label resources each exist in both files with a non-empty, distinct Spanish value in `values-es`

#### Scenario: The error state reuses existing string resources
- **WHEN** `ProfileUiState.Error` is rendered
- **THEN** the displayed error text and retry button label are sourced from `product_list_error_message` and `product_list_retry_button` respectively, not new duplicate resources
