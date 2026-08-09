# FakeStore

A native Android app built with Kotlin and Jetpack Compose that consumes the [Fake Store API](https://fakestoreapi.com/) to browse and display e-commerce product data.

## Tech Stack

- **Kotlin 2.2** with Jetpack Compose and Material 3
- **MVI architecture** (Model-View-Intent) with Clean Architecture layers
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

The app follows **Clean Architecture** with three layers:

- **data/** — API clients, DTOs, repository implementations, and mappers
- **domain/** — Use cases and repository interfaces (pure Kotlin, no Android dependencies)
- **ui/** — Composable screens, ViewModels (MVI), theme, and navigation

## Build & Release

Automated via Fastlane:

```bash
bundle exec fastlane test    # Run checks
bundle exec fastlane build   # Build debug APK
bundle exec fastlane beta    # Upload to Play Store internal track
bundle exec fastlane deploy  # Upload to Play Store production track
```

## Improvements & Clarifications

1. REST API does not support pagination, it should be added as  ti currently limits the scalability.
2. Requirements specify persistence for favourites but not for products, requirements do not mention caching or offline first approach. Accepted but should be clarified with Product. 