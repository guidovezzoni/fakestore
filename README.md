# FakeStore

A native Android app built with Kotlin and Jetpack Compose that consumes the [Fake Store API](https://fakestoreapi.com/) to browse and display e-commerce product data.

## Tech Stack

- **Kotlin 2.2** with Jetpack Compose and Material 3
- **MVI architecture** (Model-View-Intent) with Clean Architecture layers
- **Multi-module**: `build-logic` (convention plugins), `:core`, `:domain`, `:data`, `:app`
- **Networking**: Retrofit + OkHttp + kotlinx-serialization
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
- **:domain** — Domain models (`Product`, `Rating`), repository interface (`ProductRepository`), use cases (`GetProductsUseCase`) — pure Kotlin, no Android dependencies
- **:data** — DTOs (`ProductDto`, `RatingDto`), mapper, `ApiService`, `ProductRepositoryImpl`
- **:app** — Jetpack Compose UI, ViewModels (MVI), theme, navigation

## Build & Release

Automated via Fastlane:

```bash
bundle exec fastlane test    # Run checks
bundle exec fastlane build   # Build debug APK
bundle exec fastlane beta    # Upload to Play Store internal track
bundle exec fastlane deploy  # Upload to Play Store production track
```

## Product Clarifications

1. REST API does not support pagination, it should be added as  it currently limits the app scalability.
2. Requirements specify persistence for favourites but not for products, requirements do not mention caching or offline first approach. Accepted but should be clarified with Product. 

## Technical Improvements

1. kover currently excludes also by annotatedBy, however the annotatedBy filter excludes the entire annotated class, excluding code that genuinely might need unit test, this needs to be reviewed and replaced by a more accurate exclusion.

## Process Improvements

1. PRs are fairly big, this is a tradeoff due to the size of the project and the time available, in normal situations PRs should be sized in such a way that can be easily reviewed by peers.
