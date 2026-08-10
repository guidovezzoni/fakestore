## 1. Build Configuration (Prerequisite)

- [x] 1.1 Confirm the exact KSP version matching Kotlin `2.2.10` (expected `2.2.10-2.0.2`, verify against `https://github.com/google/ksp/releases`) and the current Hilt release (`2.60` per Context7-verified `dagger.dev/hilt/gradle-setup.html`)
- [x] 1.2 Add `hilt` and `ksp` versions, `[libraries]` entries (`hilt-android`, `hilt-compiler`, `hilt-android-testing`), and `[plugins]` entries (`hilt-android`, `ksp`) to `gradle/libs.versions.toml`
- [x] 1.3 Add `alias(libs.plugins.hilt.android) apply false` and `alias(libs.plugins.ksp) apply false` to the root `build.gradle.kts`
- [x] 1.4 Apply `alias(libs.plugins.hilt.android)` and `alias(libs.plugins.ksp)` in `app/build.gradle.kts`; add `implementation(project(":core"))`, `implementation(project(":domain"))`, `implementation(project(":data"))`; add `implementation(libs.hilt.android)`, `ksp(libs.hilt.compiler)`, `androidTestImplementation(libs.hilt.android.testing)`, `kspAndroidTest(libs.hilt.compiler)`
- [x] 1.5 Confirm no `build-logic` convention plugin changes are needed — `app/build.gradle.kts` is not built via a convention plugin (it applies plugins directly), so Hilt/KSP wiring is local to `:app` only; `AndroidLibraryConventionPlugin`/`KotlinLibraryConventionPlugin` (used by `:core`/`:domain`/`:data`) stay untouched

## 2. NetworkClient Conversion (BDD)

- [x] 2.1 Write test: GIVEN a newly constructed `NetworkClient` instance WHEN its `retrofit` property's base URL is inspected THEN it equals `https://fakestoreapi.com/` (matching `BuildConfig.BASE_URL`), updating `NetworkClientTest` in `core/src/test/kotlin/com/guidovezzoni/fakestore/core/network/NetworkClientTest.kt` to instantiate `NetworkClient()` instead of referencing the object
- [x] 2.2 Implement: convert `core/src/main/kotlin/com/guidovezzoni/fakestore/core/network/NetworkClient.kt` from `object NetworkClient` to `class NetworkClient`, with construction logic (timeouts, `Json`, converter factory, `BuildConfig.BASE_URL`) unchanged

## 3. Network Provisioning (BDD)

- [x] 3.1 Write test: GIVEN `NetworkModule.provideNetworkClient()` and `NetworkModule.provideRetrofit(networkClient)` are called directly without booting the Hilt graph WHEN the returned `Retrofit`'s base URL is inspected THEN it equals `NetworkClient`'s configured base URL (`https://fakestoreapi.com/`), in `app/src/test/java/com/guidovezzoni/fakestore/di/NetworkModuleTest.kt`
- [x] 3.2 Implement: `app/src/main/java/com/guidovezzoni/fakestore/di/NetworkModule.kt` — `@Module @InstallIn(SingletonComponent::class) object NetworkModule` with `@Provides @Singleton fun provideNetworkClient(): NetworkClient = NetworkClient()` and `@Provides @Singleton fun provideRetrofit(networkClient: NetworkClient): Retrofit = networkClient.retrofit`

## 4. Analytics Provisioning (BDD)

- [x] 4.1 Write test: GIVEN the analytics provisioning function is invoked with `isDebug = true` WHEN the resulting `AnalyticsClient.logEvent("app_open")` is called THEN a registered `DebugAnalyticsProvider`-equivalent test double receives the event (assert via a mock `AnalyticsProvider` swapped in, or assert the registered-provider count/behaviour reflects one active provider), in `app/src/test/java/com/guidovezzoni/fakestore/di/AnalyticsModuleTest.kt`
- [x] 4.2 Write test: GIVEN the analytics provisioning function is invoked with `isDebug = false` WHEN the resulting `AnalyticsClient.logEvent("app_open")` is called THEN no debug provider is registered and no debug-only side effect occurs, in `AnalyticsModuleTest`
- [x] 4.3 Implement: `app/src/main/java/com/guidovezzoni/fakestore/di/AnalyticsModule.kt` — `@Module @InstallIn(SingletonComponent::class) object AnalyticsModule` with `@Provides @Singleton fun provideAnalyticsClient(): AnalyticsClient = provideAnalyticsClient(BuildConfig.DEBUG)` delegating to `internal fun provideAnalyticsClient(isDebug: Boolean): AnalyticsClient`, which constructs `AnalyticsClient()` and calls `register(DebugAnalyticsProvider())` only when `isDebug` is true

## 5. Data Layer Provisioning (BDD)

- [x] 5.1 Write test: GIVEN `DataModule.provideProductRepository(apiService)` is called directly with a mock `ApiService` WHEN the return value's type is inspected THEN it is `ProductRepositoryImpl`, in `app/src/test/java/com/guidovezzoni/fakestore/di/DataModuleTest.kt`
- [x] 5.2 Write test: GIVEN `DataModule.provideGetProductsUseCase(repository)` is called directly with a mock `ProductRepository` returning a known product list WHEN the returned `GetProductsUseCase` is invoked and collected THEN it emits `Result.success` wrapping that product list, in `DataModuleTest`
- [x] 5.3 Write test: GIVEN `DataModule.provideApiService(retrofit)` is called directly with `NetworkModule.provideRetrofit(NetworkModule.provideNetworkClient())` WHEN the return value's type is inspected THEN it is a non-null `ApiService` proxy, in `DataModuleTest`
- [x] 5.4 Implement: `app/src/main/java/com/guidovezzoni/fakestore/di/DataModule.kt` — `@Module @InstallIn(SingletonComponent::class) object DataModule` with `@Provides fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)`, `@Provides fun provideProductRepository(apiService: ApiService): ProductRepository = ProductRepositoryImpl(apiService)`, `@Provides fun provideGetProductsUseCase(repository: ProductRepository): GetProductsUseCase = GetProductsUseCase(repository)`

## 6. Application & Entry-Point Integration

- [x] 6.1 Create `app/src/main/java/com/guidovezzoni/fakestore/FakeStoreApplication.kt` — `@HiltAndroidApp class FakeStoreApplication : Application()`
- [x] 6.2 Update `app/src/main/AndroidManifest.xml` — add `android:name=".FakeStoreApplication"` to the `<application>` element
- [x] 6.3 Add `@AndroidEntryPoint` annotation to `MainActivity` in `app/src/main/java/com/guidovezzoni/fakestore/MainActivity.kt`
- [x] 6.4 Add `*.FakeStoreApplication` to the Kover `classes` excludes list in `app/build.gradle.kts` (alongside the existing `*.MainActivity` entry)

## 7. Dependency Graph Assembly (BDD)

- [x] 7.1 Create `app/src/androidTest/java/com/guidovezzoni/fakestore/HiltTestRunner.kt` — custom `AndroidJUnitRunner` overriding `newApplication(cl, appName, context)` to return `HiltTestApplication`
- [x] 7.2 Set `testInstrumentationRunner = "com.guidovezzoni.fakestore.HiltTestRunner"` in `app/build.gradle.kts` `defaultConfig` (replacing `androidx.test.runner.AndroidJUnitRunner`)
- [x] 7.3 Write test: GIVEN a `@HiltAndroidTest`-annotated instrumented test with `@get:Rule val hiltRule = HiltAndroidRule(this)` running under `HiltTestApplication` WHEN `@Inject lateinit var productRepository: ProductRepository` is injected via `hiltRule.inject()` THEN `productRepository` is non-null and is an instance of `ProductRepositoryImpl`, in `app/src/androidTest/java/com/guidovezzoni/fakestore/HiltDependencyGraphTest.kt`
- [x] 7.4 Implement: confirm the full graph (`FakeStoreApplication` → `SingletonComponent` → `NetworkModule` + `AnalyticsModule` + `DataModule`) assembles with no missing/duplicate/circular binding, resolving `HiltDependencyGraphTest`'s injection — no additional production code should be needed if sections 3–6 are complete; fix any binding gap surfaced by this test

## 8. Final Verification

- [x] 8.1 Run `./gradlew assembleDebug` and confirm the Hilt/KSP-annotated build compiles cleanly
- [x] 8.2 Run `./gradlew test` and confirm all existing tests (`AnalyticsClientTest`, `DebugAnalyticsProviderTest`, `ProductMapperTest`, `ProductRepositoryImplTest`, `GetProductsUseCaseTest`) pass unmodified, alongside the new `NetworkClientTest`, `NetworkModuleTest`, `AnalyticsModuleTest`, `DataModuleTest`
- [x] 8.3 Run `./gradlew detektDebug` and resolve any violations
- [x] 8.4 Run `./gradlew koverVerify` (or `koverHtmlReportDebug`) and confirm the 95% minimum bound holds across `:app`, `:core`, `:domain`, `:data`
- [x] 8.5 Run `./gradlew connectedDebugAndroidTest` on a connected device/emulator and confirm `HiltDependencyGraphTest` passes, per the project's on-device verification discipline
- [x] 8.6 Cross-check the implementation against every Acceptance Criterion in story 1.1.3: app starts with Hilt graph initialised, `NetworkClient` and `AnalyticsClient` are singleton-scoped and injectable, debug provider auto-registered in debug builds and absent in release (verified by build variant), `ProductRepository` resolves to `ProductRepositoryImpl`, `GetProductsUseCase` and `ApiService` are injectable, `MainActivity` is an `@AndroidEntryPoint`, no manual construction of graph-managed types outside `di/`, `:domain` gained no Android/Hilt dependency, all existing tests pass, and `assembleDebug`/`test`/`detektDebug`/`koverVerify` all pass
