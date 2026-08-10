# Spec: Product Catalogue Data

## Purpose

Defines the requirements for retrieving and exposing product catalogue data from the FakeStore API across the multi-module Clean Architecture layers (`:core`, `:domain`, `:data`).

---

## Requirements

### Requirement: Convention plugins centralise shared build configuration
A `build-logic` composite build SHALL provide `AndroidLibraryConventionPlugin` and `KotlinLibraryConventionPlugin`, each applying detekt and kover with the project's standard configuration (the root `config/detekt/detekt.yml`, `buildUponDefaultConfig = true`, and a 95% minimum Kover bound). `:core` and `:data` SHALL apply the Android library convention plugin; `:domain` SHALL apply the Kotlin JVM convention plugin.

#### Scenario: Android library module applies the shared convention plugin
- **WHEN** `:core` or `:data`'s `build.gradle.kts` is evaluated
- **THEN** the module applies `com.android.library`, Kotlin Android, detekt, and kover through a single `fakestore.android.library` convention plugin, with `compileSdk`/`minSdk`/`targetSdk` matching `:app`'s values and no inline duplication of detekt/kover configuration

#### Scenario: Pure Kotlin module applies the Kotlin JVM convention plugin
- **WHEN** `:domain`'s `build.gradle.kts` is evaluated
- **THEN** the module applies `kotlin("jvm")`, detekt, and kover through a single `fakestore.kotlin.library` convention plugin, and declares no Android Gradle plugin or Android dependency

### Requirement: Module dependency graph is enforced
The project SHALL define `:core`, `:domain`, and `:data` as separate Gradle modules registered in `settings.gradle.kts`, with `:data` depending on `:domain` and `:core`, and `:domain` depending on neither `:core` nor `:data` nor any Android framework artifact.

#### Scenario: Domain module has no Android or data-layer dependencies
- **WHEN** `:domain`'s `build.gradle.kts` dependencies block is inspected
- **THEN** it declares no dependency on `:core`, `:data`, or any `com.android.*`/`androidx.*` artifact

#### Scenario: Data module depends on domain and core
- **WHEN** `:data`'s `build.gradle.kts` dependencies block is inspected
- **THEN** it declares `implementation(project(":domain"))` and `implementation(project(":core"))`

### Requirement: Product domain model
`:domain` SHALL define a pure Kotlin `Product` data class with fields `id: Int`, `title: String`, `price: Double`, `description: String`, `category: String`, `imageUrl: String`, and `rating: Rating`, with no Android, networking, or serialisation framework imports.

#### Scenario: Product model preserves rating as a value object
- **WHEN** a `Product` instance is constructed
- **THEN** its `rating` field is a `Rating` value object (not flattened `Double`/`Int` fields on `Product` itself)

### Requirement: Rating value object
`:domain` SHALL define a pure Kotlin `Rating` data class with fields `score: Double` and `count: Int`, representing a product's user rating as a coherent domain concept.

#### Scenario: Rating is constructed from score and count
- **WHEN** a `Rating` instance is constructed with `score = 3.9` and `count = 120`
- **THEN** `rating.score` equals `3.9` and `rating.count` equals `120`

### Requirement: DTO models mirror the API response shape
`:data` SHALL define `ProductDto` (fields `id`, `title`, `price`, `description`, `category`, `image`, `rating`) and `RatingDto` (fields `rate`, `count`) as JSON-deserialisable data classes matching the FakeStore `/products` response shape exactly, configured to ignore unknown JSON keys.

#### Scenario: ProductDto deserialises a well-formed API response
- **WHEN** the JSON parser deserialises a single product object from `GET https://fakestoreapi.com/products`
- **THEN** the resulting `ProductDto` has all seven fields populated, including a nested `RatingDto` with `rate` and `count`

#### Scenario: Unknown JSON fields do not fail deserialisation
- **WHEN** the API response contains a field not present in `ProductDto`
- **THEN** deserialisation succeeds and the unknown field is ignored

### Requirement: ProductDto to Product mapping
A mapper in `:data` SHALL convert a `ProductDto` (and its nested `RatingDto`) into a `Product` domain model (and its nested `Rating` value object), mapping every field, and SHALL NOT be accessible from `:domain`.

#### Scenario: Mapper converts all fields including nested rating
- **GIVEN** a `ProductDto` with `id = 1`, `title = "Backpack"`, `price = 109.95`, `description = "desc"`, `category = "men's clothing"`, `image = "https://example.com/img.png"`, and `rating = RatingDto(rate = 3.9, count = 120)`
- **WHEN** the mapper converts the DTO to a domain model
- **THEN** the resulting `Product` has `id = 1`, `title = "Backpack"`, `price = 109.95`, `description = "desc"`, `category = "men's clothing"`, `imageUrl = "https://example.com/img.png"`, and `rating = Rating(score = 3.9, count = 120)`

### Requirement: Network client configuration
`:core` SHALL provide a configured network client instance (Retrofit over OkHttp) with base URL `https://fakestoreapi.com` exposed via a `:core` `BuildConfig` field, and reasonable connect and read timeouts to prevent an indefinitely hanging coroutine.

#### Scenario: Network client is configured with the FakeStore base URL
- **WHEN** the `:core` network client is built
- **THEN** its base URL is `https://fakestoreapi.com`, sourced from `BuildConfig.BASE_URL` rather than a literal in application code

#### Scenario: Network client has explicit timeouts
- **WHEN** the `:core` network client is built
- **THEN** its OkHttp client has non-default, explicitly configured connect and read timeouts (extracted to named constants, not inline magic numbers)

### Requirement: API service contract
`:data` SHALL define an `ApiService` interface with `suspend fun getProducts(): List<ProductDto>`, backed by the `:core` network client, targeting `GET /products`.

#### Scenario: ApiService fetches the product list
- **WHEN** `ApiService.getProducts()` is invoked against a successful `GET /products` response
- **THEN** it returns a `List<ProductDto>` matching the response body

### Requirement: Product repository contract
`:domain` SHALL define a `ProductRepository` interface with `suspend fun getProducts(): List<Product>` returning domain models only, and throwing on network or parsing failure. `:data` SHALL provide `ProductRepositoryImpl`, which calls `ApiService`, applies the mapper to each DTO, and returns the mapped domain models.

#### Scenario: Repository returns mapped domain models on success
- **GIVEN** `ApiService.getProducts()` returns a list of `ProductDto`
- **WHEN** `ProductRepositoryImpl.getProducts()` is called
- **THEN** it returns a `List<Product>` where each element is the mapped equivalent of the corresponding DTO, and no `ProductDto` or `RatingDto` is exposed outside `:data`

#### Scenario: Repository propagates a network exception
- **GIVEN** `ApiService.getProducts()` throws an `IOException`
- **WHEN** `ProductRepositoryImpl.getProducts()` is called
- **THEN** the exception propagates unchanged out of `getProducts()`

### Requirement: GetProductsUseCase emits a single Result and completes
`:domain` SHALL define `GetProductsUseCase(private val repository: ProductRepository)` with `operator fun invoke(): Flow<Result<List<Product>>>`, emitting exactly one `Result.success` containing the product list on success, exactly one `Result.failure` wrapping any caught exception on error, and completing the `Flow` after that single emission in either case.

#### Scenario: Use case emits success and completes
- **GIVEN** `ProductRepository.getProducts()` returns a list of products
- **WHEN** `GetProductsUseCase()` is invoked and collected
- **THEN** exactly one `Result.success` containing that product list is emitted, and the `Flow` completes with no further emissions

#### Scenario: Use case emits failure and completes on repository exception
- **GIVEN** `ProductRepository.getProducts()` throws an exception (network error, timeout, or malformed JSON)
- **WHEN** `GetProductsUseCase()` is invoked and collected
- **THEN** exactly one `Result.failure` wrapping that exception is emitted, no exception escapes the `Flow` collector, and the `Flow` completes

### Requirement: No unhandled exceptions reach the use case caller
Every network or parsing failure path — no connectivity, timeout, malformed JSON, non-2xx HTTP response — SHALL be caught at or below the `GetProductsUseCase` boundary and surfaced as `Result.failure`, never as an unhandled exception thrown to the caller.

#### Scenario: HTTP error response surfaces as Result.failure
- **GIVEN** the FakeStore API responds with a non-2xx HTTP status
- **WHEN** `GetProductsUseCase()` is invoked and collected
- **THEN** the collector receives `Result.failure` wrapping an exception describing the HTTP error, and no exception is thrown out of the collection call
