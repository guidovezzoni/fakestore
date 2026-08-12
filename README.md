# FakeStore

A native Android app built with Kotlin and Jetpack Compose that consumes the [Fake Store API](https://fakestoreapi.com/) to browse and display e-commerce product data.

## Tech Stack

- **Kotlin 2.2** with Jetpack Compose and Material 3
- **MVI architecture** (Model-View-Intent) with Clean Architecture layers
- **Navigation**: Jetpack Navigation Compose 2.9.8 with type-safe (`@Serializable`) routes and a 3-tab bottom navigation bar
- **Multi-module**: `build-logic` (convention plugins), `:core`, `:domain`, `:data`, `:app`
- **Networking**: Retrofit + OkHttp + kotlinx-serialization
- **Local Persistence**: Room 2.7 with KSP annotation processing; favourites stored in `FavouritesDatabase`
- **Image Loading**: Coil 3 (with OkHttp network engine)
- **Dependency Injection**: Hilt (Dagger-Hilt 2.60) + KSP; modules in `:app/di/`
- **Detekt** for static analysis (with Compose rules)
- **Kover** for code coverage (95% minimum)
- **Fastlane** for build and release automation

## Requirements

- Android Studio (latest stable)
- JDK 11+
- Android SDK with API 37
- Min SDK: 24

## Getting Started

```bash
# Clone the repository
git clone <repo-url>
cd fakestore

# Build the debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run static analysis
./gradlew detektDebug

# Run all checks (tests + detekt + lint)
./gradlew check
```

## Project Structure

The project uses a multi-module Clean Architecture with four Gradle modules:

- **build-logic/** — Convention plugins (`fakestore.android.library`, `fakestore.kotlin.library`) centralising Detekt, Kover, and compile configuration
- **:core** — Network client (Retrofit + OkHttp), `BuildConfig.BASE_URL`; analytics abstraction (`AnalyticsProvider` interface, `AnalyticsClient` dispatcher, `DebugAnalyticsProvider`)
- **:domain** — Domain models (`Product`, `Rating`), repository interfaces (`ProductRepository`, `FavouritesRepository`), use cases (`GetProductsUseCase`, `GetFavouriteIdsUseCase`, `ToggleFavouriteUseCase`) — pure Kotlin, no Android dependencies
- **:data** — DTOs, mappers, `ApiService`, `ProductRepositoryImpl`; Room database (`FavouritesDatabase`, `FavouriteDao`, `FavouriteEntity`, `FavouritesRepositoryImpl`) — KSP annotation processing
- **:app** — Jetpack Compose UI, ViewModels (MVI), theme, navigation; Hilt DI modules (`NetworkModule`, `AnalyticsModule`, `DataModule`, `DatabaseModule`), `FakeStoreApplication` (`@HiltAndroidApp`)

## Build & Release

Automated via Fastlane:

```bash
bundle exec fastlane test    # Run checks
bundle exec fastlane build   # Build debug APK
bundle exec fastlane beta    # Upload to Play Store internal track
bundle exec fastlane deploy  # Upload to Play Store production track
```

## Limitations and Assumptions

### Assumptions
1. Currently, the user selection is not possible, the current us is hardcoded in order to reduce the scope of the project and fit the deadline.

### Limitations
1. The current REST API does not support pagination, it should be added as  it currently limits the app scalability. The app implementation is based on the current API structure and the implied assumption that the number of products is limited (currently 20). Accordingly, the combination of products and favourites is based on the same assumption. All this structure will have to be re-viewed once pagination is made available.
2. User password is returned with the username, that is likely unsafe.


### Product Clarifications

1. Requirements specify persistence for favourites but not for products, requirements do not mention caching or offline first approach. Accepted but should be clarified with Product.
2. Currency for product prices is hardcoded to USD. Ideally the price should come with a currency field. 

### Technical Improvements

1. kover currently excludes also by annotatedBy, however the annotatedBy filter excludes the entire annotated class, excluding code that genuinely might need unit test, this needs to be reviewed and replaced by a more accurate exclusion.
2. Analytics events are currently implemented in a rough way - error conditions are not logged, events are strings, parameters usage can be tailored further.

### SDD/SDLC Process Improvements

1. PRs are fairly big, this is a tradeoff due to the size of the project and the time available, in normal situations PRs should be sized in such a way that can be easily reviewed by peers.
