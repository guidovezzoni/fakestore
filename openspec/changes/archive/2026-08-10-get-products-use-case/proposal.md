## Why

The FakeStore app currently has no data layer: only the `:app` module exists, with no way to fetch product data from the network. The Product Catalogue epic (downstream stories 1.2.1, 1.2.2) needs a domain-layer `GetProductsUseCase` that exposes `Flow<Result<List<Product>>>` so the UI can react to loading, success, and error states without touching networking or DTO concerns directly. This is the foundational story that establishes the multi-module Clean Architecture (`:core`, `:domain`, `:data`) the rest of the epic builds on.

## What Changes

- Add a `build-logic` composite build with `AndroidLibraryConventionPlugin` and `KotlinLibraryConventionPlugin` to centralise compileSdk/minSdk, Java/Kotlin compile options, detekt, and kover configuration across modules.
- Create three new Gradle modules — `:core`, `:domain`, `:data` — and register them in `settings.gradle.kts` via `pluginManagement { includeBuild("build-logic") }`.
- Add Retrofit, OkHttp, kotlinx-serialization, MockK, and kotlinx-coroutines-test dependencies to `gradle/libs.versions.toml`.
- `:core` provides a configured Retrofit instance with base URL `https://fakestoreapi.com` (via `BuildConfig`), sane connect/read timeouts, and JSON conversion via kotlinx-serialization.
- `:domain` (pure Kotlin JVM, no Android imports) defines the `Product` domain model, the `Rating` value object, the `ProductRepository` interface, and `GetProductsUseCase`.
- `:data` defines `ProductDto`/`RatingDto` mirroring the API response shape, `ProductMapper` (DTO → domain), `ApiService` (`suspend fun getProducts(): List<ProductDto>`), and `ProductRepositoryImpl`.
- All network/parsing errors are caught at the repository/use-case boundary and surfaced as `Result.failure` — no unhandled exceptions reach the ViewModel.
- Unit tests (JUnit 4 + MockK) for `ProductMapper`, `ProductRepositoryImpl`, and `GetProductsUseCase`, targeting 95%+ coverage.
- `:app` does **not** gain a dependency on `:domain` or `:data` in this story — DI wiring is deferred to story 1.1.3.

## Capabilities

### New Capabilities
- `product-catalogue-data`: End-to-end data pipeline for fetching the product catalogue — network client configuration (`:core`), DTOs and mapping (`:data`), and the domain-layer `Product`/`Rating` models, `ProductRepository` contract, and `GetProductsUseCase` (`:domain`), including error handling that surfaces failures as `Result.failure`.

### Modified Capabilities
- None. This is a greenfield addition; no existing specs are affected.

## Impact

- **New Gradle modules**: `build-logic`, `:core`, `:domain`, `:data`.
- **Modified build files**: root `settings.gradle.kts` (composite build + module includes), `gradle/libs.versions.toml` (new dependency + plugin aliases). `app/build.gradle.kts` is unaffected in this story.
- **New source code**: `Product.kt`, `Rating.kt`, `ProductRepository.kt`, `GetProductsUseCase.kt` (`:domain`); `ProductDto.kt`, `RatingDto.kt`, `ProductMapper.kt`, `ApiService.kt`, `ProductRepositoryImpl.kt` (`:data`); network client configuration (`:core`).
- **New dependencies**: Retrofit, OkHttp, kotlinx-serialization (+ retrofit converter), MockK, kotlinx-coroutines-test.
- **No UI, no DI wiring, no analytics** — those are covered by stories 1.2.1, 1.1.3, and 1.1.2 respectively.
- **No breaking changes** — nothing existing depends on the new modules yet.
