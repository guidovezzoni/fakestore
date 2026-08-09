## 1. Build-Logic Composite Build (Prerequisites)

- [ ] 1.1 Create `build-logic/settings.gradle.kts` with `dependencyResolutionManagement` (google, mavenCentral, gradlePluginPortal repositories) exposing the root `gradle/libs.versions.toml` as version catalog `libs` via `versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }`
- [ ] 1.2 Create `build-logic/build.gradle.kts` applying the `kotlin-dsl` plugin, with dependencies on the AGP and Kotlin Gradle plugin artifacts, and register `fakestore.android.library` and `fakestore.kotlin.library` plugin IDs via `gradlePlugin { plugins { ... } }`
- [ ] 1.3 Implement `build-logic/src/main/kotlin/AndroidLibraryConventionPlugin.kt`: applies `com.android.library`, `org.jetbrains.kotlin.android`, detekt, kover; configures `compileSdk`/`minSdk`/`targetSdk` matching `:app`'s current values, Java 11 `compileOptions`, `jvmTarget = "11"`, detekt against `config/detekt/detekt.yml` with `buildUponDefaultConfig = true`, and Kover exclusion filters + `minBound(95)` matching `app/build.gradle.kts`
- [ ] 1.4 Implement `build-logic/src/main/kotlin/KotlinLibraryConventionPlugin.kt`: applies `kotlin("jvm")`, detekt, kover; configures `jvmTarget = "11"`, the same detekt config as above, and Kover exclusion filters + `minBound(95)`

## 2. Module Scaffolding & Gradle Wiring (Prerequisites)

- [ ] 2.1 Add `pluginManagement { includeBuild("build-logic") }` and `include(":core", ":domain", ":data")` to the root `settings.gradle.kts`
- [ ] 2.2 Add to `gradle/libs.versions.toml`: Retrofit, OkHttp, kotlinx-serialization-json, retrofit2-kotlinx-serialization-converter, MockK, kotlinx-coroutines-core, kotlinx-coroutines-test version + library aliases, the `kotlin.plugin.serialization` plugin alias, and the two `fakestore.android.library` / `fakestore.kotlin.library` convention plugin aliases
- [ ] 2.3 Create `domain/build.gradle.kts` applying `fakestore.kotlin.library`, with `kotlinx-coroutines-core` as the only production dependency and MockK + kotlinx-coroutines-test as test dependencies
- [ ] 2.4 Create `core/build.gradle.kts` applying `fakestore.android.library`, with `namespace = "com.guidovezzoni.fakestore.core"`, `buildFeatures { buildConfig = true }`, a `buildConfigField("String", "BASE_URL", "\"https://fakestoreapi.com\"")`, and Retrofit/OkHttp/kotlinx-serialization-converter dependencies
- [ ] 2.5 Create `data/build.gradle.kts` applying `fakestore.android.library` and the `kotlin.plugin.serialization` plugin, with `namespace = "com.guidovezzoni.fakestore.data"`, `implementation(project(":domain"))`, `implementation(project(":core"))`, kotlinx-serialization-json, and MockK + kotlinx-coroutines-test as test dependencies

## 3. Domain Models & Contracts (Prerequisites)

- [ ] 3.1 Implement `domain/src/main/kotlin/.../Rating.kt` — pure Kotlin data class with `score: Double`, `count: Int`
- [ ] 3.2 Implement `domain/src/main/kotlin/.../Product.kt` — pure Kotlin data class with `id: Int`, `title: String`, `price: Double`, `description: String`, `category: String`, `imageUrl: String`, `rating: Rating`
- [ ] 3.3 Define `domain/src/main/kotlin/.../ProductRepository.kt` — interface with `suspend fun getProducts(): List<Product>`

## 4. DTO Models (Prerequisites)

- [ ] 4.1 Implement `data/src/main/kotlin/.../RatingDto.kt` — `@Serializable` data class with `rate: Double`, `count: Int` mapped to JSON keys `rate`/`count`
- [ ] 4.2 Implement `data/src/main/kotlin/.../ProductDto.kt` — `@Serializable` data class mirroring the API shape (`id`, `title`, `price`, `description`, `category`, `image`, `rating: RatingDto`)

## 5. Network Client Configuration (:core) (BDD)

- [ ] 5.1 Write test: GIVEN the `:core` network client factory WHEN the `Retrofit` instance is built THEN its `baseUrl()` equals `BuildConfig.BASE_URL`, in `NetworkClientTest`
- [ ] 5.2 Implement: `core/src/main/kotlin/.../NetworkClient.kt` providing a configured `OkHttpClient` (connect/read timeout `private const val`s, no cleartext fallback) and a `Retrofit` instance built with `BuildConfig.BASE_URL` and a kotlinx-serialization converter factory (`Json { ignoreUnknownKeys = true }`)

## 6. API Service Contract (:data) (Prerequisite)

- [ ] 6.1 Define `data/src/main/kotlin/.../ApiService.kt` — Retrofit interface with `@GET("products") suspend fun getProducts(): List<ProductDto>`

## 7. Product Mapper (:data) (BDD)

- [ ] 7.1 Write test: GIVEN a `ProductDto` with a nested `RatingDto` WHEN `ProductMapper` maps it to a domain model THEN every field is correctly mapped, including `RatingDto` → `Rating`, in `ProductMapperTest`
- [ ] 7.2 Implement: `data/src/main/kotlin/.../ProductMapper.kt` — internal-visibility mapper converting `ProductDto` → `Product` and `RatingDto` → `Rating`, not exposed to `:domain`

## 8. Product Repository Implementation (:data) (BDD)

- [ ] 8.1 Write test: GIVEN a mocked `ApiService.getProducts()` returns a list of `ProductDto` WHEN `ProductRepositoryImpl.getProducts()` is called THEN it returns the mapped `List<Product>`, in `ProductRepositoryImplTest`
- [ ] 8.2 Write test: GIVEN a mocked `ApiService.getProducts()` throws an `IOException` WHEN `ProductRepositoryImpl.getProducts()` is called THEN the exception propagates unchanged, in `ProductRepositoryImplTest`
- [ ] 8.3 Implement: `data/src/main/kotlin/.../ProductRepositoryImpl.kt` implementing `ProductRepository`, calling `ApiService.getProducts()` and applying `ProductMapper` to each element

## 9. Get Products Use Case (:domain) (BDD)

- [ ] 9.1 Write test: GIVEN a mocked `ProductRepository.getProducts()` returns a product list WHEN `GetProductsUseCase()` is invoked and collected THEN it emits exactly one `Result.success` with that list and the `Flow` completes, in `GetProductsUseCaseTest`
- [ ] 9.2 Write test: GIVEN a mocked `ProductRepository.getProducts()` throws an exception WHEN `GetProductsUseCase()` is invoked and collected THEN it emits exactly one `Result.failure` wrapping that exception, no exception escapes the collector, and the `Flow` completes, in `GetProductsUseCaseTest`
- [ ] 9.3 Implement: `domain/src/main/kotlin/.../GetProductsUseCase.kt` — `operator fun invoke(): Flow<Result<List<Product>>>` using `flow { emit(Result.success(repository.getProducts())) }.catch { emit(Result.failure(it)) }`

## 10. Integration & Final Verification

- [ ] 10.1 Verify module dependency graph: `:domain` has zero Android/framework imports; `:data` depends only on `:domain` + `:core`; `:app`'s `build.gradle.kts` is unchanged (no `:domain`/`:data` dependency added in this story)
- [ ] 10.2 Run `./gradlew detektDebug` and resolve any violations (all literals — timeouts, base URL — extracted to named constants)
- [ ] 10.3 Run `./gradlew test` and confirm all new tests pass
- [ ] 10.4 Run `./gradlew koverVerify` (or `koverHtmlReportDebug` for a detailed view) and confirm ≥95% coverage on `:core`, `:domain`, and `:data`
- [ ] 10.5 Cross-check the implementation against every Acceptance Criterion and Definition of Done item in `docs/userstories/1.1.1-Get-Products-Use-Case-WIP.md`
