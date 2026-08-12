# AGENTS.md

This file contains guidelines and commands for agentic coding agents working in this repository.

## External File Loading

CRITICAL: When you encounter a file reference (e.g., @docs/guidelines/guidelines-android.md), use your Read tool to load it on a need-to-know basis. They're relevant to the SPECIFIC task at hand.

Instructions:

- Do NOT preemptively load all references - use lazy loading based on actual need
- When loaded, treat content as mandatory instructions that override defaults
- Follow references recursively when needed
- If a file reference cannot be found, always notify the user.

### Android Guidelines

For Native Android code style and best practices: @docs/guidelines/guidelines-android.md

### Git Guidelines

For git operations and commit conventions: @docs/guidelines/guidelines-git.md

### General Guidelines

Read the following file immediately as it's relevant to all workflows: @docs/guidelines/guidelines-process.md

## Project Overview

FakeStore is a native Android application built with Kotlin and Jetpack Compose. It serves as an interview/demo project that consumes the [Fake Store API](https://fakestoreapi.com/) to display e-commerce product data.

### Tech Stack

- **Language:** Kotlin 2.2
- **UI:** Jetpack Compose with Material 3
- **Architecture:** MVI (Model-View-Intent) with Clean Architecture layers (data / domain / ui)
- **Multi-module:** `build-logic` composite build with `fakestore.android.library` and `fakestore.kotlin.library` convention plugins; modules `:core`, `:domain`, `:data`, `:app`
- **Navigation:** Jetpack Navigation Compose 2.9.8 with type-safe (`@Serializable`) routes; `MainScreen` hosts a `NavHost` with `AppDestination` sealed-interface destinations
- **Networking:** Retrofit + OkHttp + kotlinx-serialization (converter: `retrofit2-kotlinx-serialization-converter`)
- **Image Loading:** Coil 3 (`coil-compose` + `coil-network-okhttp`)
- **Dependency Injection:** Hilt 2.60 (Dagger-Hilt) + KSP 2.2.10-2.0.2; `@Provides` modules in `:app/di/`; entry points: `FakeStoreApplication` (`@HiltAndroidApp`), `MainActivity` (`@AndroidEntryPoint`)
- **Build system:** Gradle (Kotlin DSL) with version catalog (`gradle/libs.versions.toml`)
- **Min SDK:** 24 — **Target SDK:** 37
- **Static analysis:** Detekt 2.x with Compose rules plugin
- **Code coverage:** Kover (95% minimum bound)
- **CI / Release:** Fastlane (test, build, beta, deploy lanes)

### Module Structure

```
fakestore/
├── build-logic/                # Convention plugins (Gradle composite build)
│   └── src/main/kotlin/
│       ├── AndroidLibraryConventionPlugin.kt   # fakestore.android.library
│       └── KotlinLibraryConventionPlugin.kt    # fakestore.kotlin.library
├── core/                       # Network client (Retrofit/OkHttp), BuildConfig.BASE_URL; analytics abstraction
│   └── src/main/kotlin/com/guidovezzoni/fakestore/core/
│       ├── network/NetworkClient.kt
│       └── analytics/          # AnalyticsProvider (interface), AnalyticsClient (dispatcher), DebugAnalyticsProvider
├── domain/                     # Pure Kotlin — no Android dependencies
│   └── src/main/kotlin/com/guidovezzoni/fakestore/domain/
│       ├── model/              # Product, Rating
│       ├── repository/         # ProductRepository, FavouritesRepository interfaces
│       └── usecase/            # GetProductsUseCase, GetFavouriteIdsUseCase, ToggleFavouriteUseCase
├── data/                       # Data layer — implements domain contracts
│   └── src/main/kotlin/com/guidovezzoni/fakestore/data/
│       ├── model/              # ProductDto, RatingDto (@Serializable)
│       ├── network/            # ApiService (Retrofit)
│       ├── mapper/             # ProductMapper (internal)
│       ├── repository/         # ProductRepositoryImpl, FavouritesRepositoryImpl
│       └── database/           # FavouritesDatabase (Room), FavouriteDao, FavouriteEntity
└── app/                        # Android application module
    └── src/main/java/com/guidovezzoni/fakestore/
        ├── di/                 # Dependency injection modules (NetworkModule, AnalyticsModule, DataModule, DatabaseModule)
        ├── ui/
        │   ├── navigation/     # AppDestination sealed interface (type-safe nav routes)
        │   ├── screens/        # Feature screen composables (MainScreen, BottomNavigationBar, ProductListScreen, ProductListItemCard, FavouritesScreen, ProfileScreen)
        │   ├── viewmodel/      # MVI ViewModels (MainViewModel, ProductListViewModel, FavouritesViewModel)
        │   ├── state/          # UiState data classes and sealed interfaces (MainUiState, ProductListUiState, FavouritesUiState, ProductListItem)
        │   ├── intent/         # UiIntent sealed interfaces (MainUiIntent, ProductListUiIntent, FavouritesUiIntent)
        │   ├── effect/         # UiEffect sealed interfaces (MainUiEffect, ProductListUiEffect, FavouritesUiEffect)
        │   ├── util/           # Formatting and helper functions
        │   └── theme/          # Material 3 theme (Color, Theme, Type)
```

### Key Commands

| Task | Command |
|------|---------|
| Clean build | `./gradlew clean` |
| Debug build | `./gradlew assembleDebug` |
| Unit tests | `./gradlew test` |
| Static analysis | `./gradlew detektDebug` |
| Full check (tests + detekt + lint) | `./gradlew check` |
| Coverage report | `./gradlew koverHtmlReportDebug` |
| Compose UI tests | `./gradlew connectedDebugAndroidTest` |
| Fastlane test lane | `bundle exec fastlane test` |
| Fastlane debug build | `bundle exec fastlane build` |
