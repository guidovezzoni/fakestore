## Why

`:app` currently has zero project dependencies — `NetworkClient` is a bare Kotlin `object`, `AnalyticsClient` is never instantiated in production code, and `ProductRepositoryImpl`/`GetProductsUseCase` (both already constructor-injected) have no call site at all. Every future screen would otherwise need to hand-wire this graph itself (`ProductRepositoryImpl(ApiService::class...)`, `AnalyticsClient().register(...)`, etc.), duplicating object construction across ViewModels and coupling the UI layer to concrete implementation classes. This story wires Hilt end-to-end so the network client, analytics client, repository, and use case are provided through a managed dependency graph exactly once, and so the first ViewModel (not yet written) can simply declare `@Inject constructor(...)` with no additional DI plumbing.

## What Changes

- Add Hilt (`com.google.dagger:hilt-android` / `hilt-android-gradle-plugin`) and KSP (`com.google.devtools.ksp`) to `gradle/libs.versions.toml`, applied with `apply false` in the root `build.gradle.kts`, and applied (with the accompanying project dependencies) only in `app/build.gradle.kts`. `:core`, `:domain`, and `:data` gain no new plugins, annotations, or dependencies — they stay framework-free, as constructor-injectable as they already are.
- Add `app` module dependencies: `implementation(project(":core"))`, `implementation(project(":domain"))`, `implementation(project(":data"))` — currently absent.
- Convert `core/.../network/NetworkClient.kt` from a Kotlin `object` to a plain class (`class NetworkClient`) so it can be instantiated by a `@Provides` function; its internal OkHttp/Retrofit construction logic is unchanged. Update `NetworkClientTest` to instantiate the class directly.
- Add three `@InstallIn(SingletonComponent::class)` Hilt modules under `app/src/main/java/com/guidovezzoni/fakestore/di/`, each a Kotlin `object` (so `@Provides` functions are callable directly in unit tests without booting the Hilt graph):
  - `NetworkModule` — `@Provides @Singleton` for `NetworkClient` and `Retrofit`.
  - `AnalyticsModule` — `@Provides @Singleton` for `AnalyticsClient`; the debug-provider registration is gated by `BuildConfig.DEBUG` inside an extracted, independently-testable `internal` function that takes `isDebug: Boolean` as a parameter, and registers `DebugAnalyticsProvider` (existing `register()` API, unchanged) only when true.
  - `DataModule` — `@Provides` for `ApiService` (via `Retrofit.create(...)`), `ProductRepository` (bound to `ProductRepositoryImpl`), and `GetProductsUseCase`.
- Add `FakeStoreApplication` (`@HiltAndroidApp`) and register it in `AndroidManifest.xml` via `android:name`.
- Annotate `MainActivity` with `@AndroidEntryPoint`, making it a Hilt injection entry point (no field/constructor injection needed yet — no ViewModels exist).
- Add a Hilt instrumented-test harness: `hilt-android-testing` + `kspAndroidTest`/`kspTest` dependencies, a custom `HiltTestRunner` (extends `AndroidJUnitRunner`, returns `HiltTestApplication`) wired via `testInstrumentationRunner`, and one `@HiltAndroidTest` androidTest class that boots the real graph and asserts the injected `ProductRepository` resolves to `ProductRepositoryImpl`.
- Update `app/build.gradle.kts` Kover excludes to add `*.FakeStoreApplication` (the class has no testable logic beyond the generated `@HiltAndroidApp` base class); the existing `*.di.*`, `dagger.hilt.*`, `*.Hilt_*`, `*_HiltModules*`, `hilt_aggregated_deps.*`, `*_Factory*`, `*_MembersInjector` exclusions already present in both `app/build.gradle.kts` and the `build-logic` convention plugins need no further change.
- New unit tests: `NetworkModuleTest`, `AnalyticsModuleTest` (both build-type branches), `DataModuleTest` — all plain JUnit, no Hilt graph boot required. New androidTest: `HiltDependencyGraphTest`.
- No changes to `:domain`'s public API, `ProductRepository`, `GetProductsUseCase`, or `ProductRepositoryImpl` — they already use constructor injection and require no modification to become Hilt-provided.

## Capabilities

### New Capabilities
- `hilt-dependency-injection`: The `:app`-level Hilt wiring — application-level graph bootstrap (`@HiltAndroidApp`), the three `@Provides` modules (`NetworkModule`, `AnalyticsModule`, `DataModule`), build-type-gated debug analytics provider registration, `MainActivity` as an `@AndroidEntryPoint`, and the instrumented test that verifies the graph assembles and resolves `ProductRepository` to `ProductRepositoryImpl`.

### Modified Capabilities
- `product-catalogue-data`: No requirement text changes — `ProductRepository`/`ProductRepositoryImpl`/`GetProductsUseCase`/`ApiService`/`NetworkClient` behaviour is unchanged. The only structural change in scope for that spec is `NetworkClient` becoming a class instead of an object, purely to support instantiation via `@Provides`; its configuration (base URL, timeouts) is unaffected.
- `analytics-client`: No requirement text changes — `AnalyticsClient`/`AnalyticsProvider`/`DebugAnalyticsProvider` behaviour, thread-safety, and fan-out semantics are unchanged. This story only adds the DI wiring around them (registration was explicitly deferred here from the analytics-client-setup story).

## Impact

- **Modified build files**: `gradle/libs.versions.toml` (Hilt + KSP versions/libraries/plugins), root `build.gradle.kts` (Hilt + KSP `apply false`), `app/build.gradle.kts` (apply Hilt + KSP, add `project(":core")`/`project(":domain")`/`project(":data")`, add Hilt runtime/compiler/testing dependencies, extend Kover excludes), `core/build.gradle.kts` unchanged (no new dependency needed for the object→class conversion).
- **Modified source code**: `core/src/main/kotlin/.../network/NetworkClient.kt` (object → class), `app/src/main/java/com/guidovezzoni/fakestore/MainActivity.kt` (`@AndroidEntryPoint`), `app/src/main/AndroidManifest.xml` (`android:name=".FakeStoreApplication"`).
- **New source code**: `app/src/main/java/com/guidovezzoni/fakestore/FakeStoreApplication.kt`, `app/src/main/java/com/guidovezzoni/fakestore/di/NetworkModule.kt`, `AnalyticsModule.kt`, `DataModule.kt`.
- **New tests**: `core/src/test/kotlin/.../network/NetworkClientTest.kt` (updated, not new), `app/src/test/java/com/guidovezzoni/fakestore/di/NetworkModuleTest.kt`, `AnalyticsModuleTest.kt`, `DataModuleTest.kt`, `app/src/androidTest/java/com/guidovezzoni/fakestore/HiltDependencyGraphTest.kt`, `app/src/androidTest/java/com/guidovezzoni/fakestore/HiltTestRunner.kt`.
- **No breaking changes to `:domain`**: it gains no dependency, no annotation, no import — the acceptance criterion "`:domain` must NOT gain Android/Hilt framework dependencies" is met by construction, since all `@Provides`/`@Module`/`@InstallIn` code lives exclusively in `:app`.
- **No changes to existing `:data`/`:domain` test files** other than the `:core` `NetworkClientTest` update required by the object→class conversion; `ProductRepositoryImplTest`, `ProductMapperTest`, `GetProductsUseCaseTest`, `AnalyticsClientTest`, `DebugAnalyticsProviderTest` all pass unmodified.
