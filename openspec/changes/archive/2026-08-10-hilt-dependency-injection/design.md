## Context

`:app` has zero project dependencies today and no DI container. `NetworkClient` (`:core`) is a Kotlin `object` built around `BuildConfig.BASE_URL`; `AnalyticsClient` (`:core`) exists but `register()` is only ever called from tests, never from production code; `ProductRepositoryImpl` (`:data`) and `GetProductsUseCase` (`:domain`) already use plain constructor injection — nothing about them needs to change to become Hilt-provided. No `Application` subclass exists yet. `gradle/libs.versions.toml` has no Hilt, KSP, or `javax.inject` entries. Kover excludes for Hilt-generated classes (`*_HiltModules*`, `dagger.hilt.*`, `*.Hilt_*`, `hilt_aggregated_deps.*`, `*.di.*`, `*_Factory*`, `*_MembersInjector`, `@HiltViewModel`) are already pre-populated in both `app/build.gradle.kts` and the `build-logic` convention plugins (`AndroidLibraryConventionPlugin.kt`, `KotlinLibraryConventionPlugin.kt`) — someone anticipated this story. No ViewModels exist yet, so this story only prepares infrastructure; `MainActivity` becomes an `@AndroidEntryPoint` with nothing to inject into it directly.

Per `dagger.dev/hilt/gradle-setup.html` (verified via Context7 against current Hilt docs), the current stable Hilt release is `2.60`, using the `com.google.dagger.hilt.android` Gradle plugin, `com.google.dagger:hilt-android`/`hilt-compiler` libraries, and KSP (`com.google.devtools.ksp`) for annotation processing — KAPT is legacy and unnecessary on Kotlin 2.2.10. Hilt's own instrumented-testing story (`dagger.dev/hilt/testing.html`, `instrumentation-testing.html`) requires a custom `AndroidJUnitRunner` that returns `HiltTestApplication`, wired via `testInstrumentationRunner` in `android.defaultConfig`, plus `@HiltAndroidTest` + `HiltAndroidRule` on the test class itself.

## Goals / Non-Goals

**Goals:**
- Hilt bootstraps the app's dependency graph at process start (`@HiltAndroidApp` on a new `FakeStoreApplication`).
- `NetworkClient`, `AnalyticsClient` are singleton-scoped and injectable; `ProductRepository` resolves to `ProductRepositoryImpl`; `GetProductsUseCase` and `ApiService` are injectable.
- `DebugAnalyticsProvider` is registered with `AnalyticsClient` in debug builds only, gated by a runtime `BuildConfig.DEBUG` check, verified by a unit test that exercises both branches without needing separate build-type test source sets.
- `MainActivity` is a Hilt injection entry point (`@AndroidEntryPoint`).
- `:domain` (and `:core`, `:data`) gain zero Hilt/Android DI framework references — every `@Provides`/`@Module`/`@InstallIn` annotation lives in `app/di/`.
- Provisioning logic (module functions) is unit-testable by direct function call, without booting the Hilt component graph — plus one instrumented test that does boot the real graph, to prove assembly end-to-end.
- All existing unit tests pass unmodified, except `NetworkClientTest`, which is mechanically updated (object → class instantiation) as a direct consequence of the one source change required in `:core`.

**Non-Goals:**
- No ViewModels are introduced or made `@HiltViewModel` — there are none yet; this story is pure infrastructure for the first one that lands.
- No production analytics SDK adapter is wired — only the existing `DebugAnalyticsProvider`.
- No `hilt-navigation-compose` — no navigation graph exists yet.
- No `@Binds`/interface-delegation style modules — the codebase currently has no interfaces that need multiple runtime implementations besides `ProductRepository`, and a single `@Provides` function returning the interface type is simpler than an abstract `@Binds` module for one binding; revisit if a second `ProductRepository` implementation appears.
- No changes to `ProductRepository`, `GetProductsUseCase`, `ProductRepositoryImpl`, `ApiService`, `AnalyticsProvider`, `DebugAnalyticsProvider`, or `AnalyticsClient`'s public API — all already constructor-injectable or side-effect-free to construct.

## Decisions

### 1. Hilt + KSP, versions and plugin placement
Use Hilt `2.60` (`com.google.dagger:hilt-android`, `com.google.dagger:hilt-android-gradle-plugin`) with KSP for annotation processing, per current Hilt Gradle-setup docs. Both plugins are declared with `apply false` in the root `build.gradle.kts` and applied only in `app/build.gradle.kts` — `:core`, `:domain`, `:data` apply neither. This directly satisfies "`:domain` must NOT gain Android/Hilt framework dependencies" and, by the same reasoning, keeps `:core`/`:data` framework-free too, matching the user's clarification that `@Provides` factory functions are "concentrated in `app/di/` only."

*Alternative considered*: applying Hilt to `:data` so `ProductRepositoryImpl` could carry `@Inject constructor`. Rejected per explicit user clarification — constructor injection annotations are not to spread into `:core`/`:domain`/`:data`; `@Provides` functions in `:app` construct these types directly instead.

### 2. `NetworkClient`: object → class
`@Provides` functions cannot instantiate a Kotlin `object` (there's nothing to construct — it's already a singleton instance). Converting `NetworkClient` to a plain class moves singleton *scoping* from the language construct (`object`) to the DI container (`@Provides @Singleton` in `NetworkModule`), which is exactly where scoping belongs once a DI container exists. The internal construction logic (OkHttp timeouts, `Json { ignoreUnknownKeys = true }`, converter factory, `BuildConfig.BASE_URL`) is untouched — only the `object` → `class` keyword and the resulting need to instantiate it changes. `NetworkClientTest` updates from `NetworkClient.retrofit` to `NetworkClient().retrofit`; no other test in the suite touches `NetworkClient`.

*Alternative considered*: leave `NetworkClient` as an `object` and have `NetworkModule` simply return `NetworkClient.retrofit` via a `@Provides` function with no parameter. Rejected — this half-measure still hard-codes global singleton state outside the DI container (defeats "network client is singleton-scoped, injectable" as a DI-graph property, not an accidental one from `object` semantics) and would make `NetworkClient` untestable in isolation from its `object`-scoped `Json`/`OkHttpClient`/`Retrofit` fields in a future story that wants a second, differently-configured instance (e.g. for a test double).

### 3. Module structure — three modules split by concern, all `object`s
- `NetworkModule` (`@Provides @Singleton fun provideNetworkClient(): NetworkClient`, `@Provides @Singleton fun provideRetrofit(networkClient: NetworkClient): Retrofit = networkClient.retrofit`)
- `AnalyticsModule` (`@Provides @Singleton fun provideAnalyticsClient(): AnalyticsClient`)
- `DataModule` (`@Provides fun provideApiService(retrofit: Retrofit): ApiService`, `@Provides fun provideProductRepository(apiService: ApiService): ProductRepository`, `@Provides fun provideGetProductsUseCase(repository: ProductRepository): GetProductsUseCase`)

All three are Kotlin `object`s annotated `@Module @InstallIn(SingletonComponent::class)`. Hilt supports `@Module` on either a `class` or an `object`; using `object` means every `@Provides` function is a static-like member callable directly from a unit test (`NetworkModule.provideRetrofit(NetworkModule.provideNetworkClient())`) with no Hilt component, no `@Inject`, no Robolectric — directly satisfying "new provisioning code should be testable without booting Hilt graph."

`ApiService` and the OkHttp client itself are not independently `@Singleton`-scoped: `ApiService` is a cheap `Retrofit.create(...)` proxy (safe to recreate; Hilt still only calls it once per `Retrofit` singleton in practice since `Retrofit` itself is `@Singleton`), and `ProductRepository`/`GetProductsUseCase` are plain stateless wrappers — scoping them adds no value and the story's acceptance criteria only require singleton scoping for the network client and analytics client.

*Alternative considered*: one flat `AppModule`. Rejected — three modules split by concern (network / analytics / data-and-use-cases) keep each file focused and testable independently, consistent with the guideline that Hilt modules should mirror the existing `di/` folder convention rather than becoming a junk drawer.

*Alternative considered*: `@Binds` abstract module for `ProductRepository → ProductRepositoryImpl`. Rejected for now — `@Binds` requires an abstract class/interface module and `ProductRepositoryImpl`'s constructor still needs `@Provides`-level construction from `ApiService` since `ProductRepositoryImpl` carries no `@Inject constructor` (per Decision 1, no DI annotations outside `:app`). A single `@Provides fun provideProductRepository(apiService: ApiService): ProductRepository = ProductRepositoryImpl(apiService)` is simpler and requires no second module.

### 4. Debug analytics provider gating via an extracted, parameterised function
`AnalyticsModule`'s `@Provides @Singleton fun provideAnalyticsClient(): AnalyticsClient` delegates to `internal fun provideAnalyticsClient(isDebug: Boolean): AnalyticsClient`, which constructs `AnalyticsClient()` and calls `.register(DebugAnalyticsProvider())` only when `isDebug` is true, then returns the client. The `@Provides` function itself passes `BuildConfig.DEBUG` (the `:app` module's generated `BuildConfig`, which is `true` for the `debug` build type and `false` for `release`, per Android Gradle Plugin defaults — `release` in `app/build.gradle.kts` has no `isDebuggable` override, so it inherits AGP's default `false`).

This is the mechanism the user specified explicitly: "a single Hilt module... runtime `BuildConfig.DEBUG` check... NOT source-set split." Extracting the `Boolean`-parameterised inner function is what makes the two required unit tests possible: `BuildConfig.DEBUG` is a compile-time-inlined `const val`, identical for every test run within one Gradle test task, so a test cannot flip it mid-run — but it *can* call `provideAnalyticsClient(isDebug = true)` and `provideAnalyticsClient(isDebug = false)` directly and assert on the resulting `AnalyticsClient`'s behaviour (e.g. that `logEvent(...)` reaches a registered `DebugAnalyticsProvider` mock in the `true` case and reaches nothing in the `false` case, using the same `mockkStatic(Log::class)`-free black-box approach `AnalyticsClientTest` already uses: assert via a spy/mock `AnalyticsProvider` registered alongside, or assert the registry size/behaviour is consistent with one provider vs. zero).

*Alternative considered*: debug/release Kotlin source sets (`app/src/debug/.../di/AnalyticsModule.kt` vs. `app/src/release/.../di/AnalyticsModule.kt`), each providing a different binding. Rejected per explicit user clarification ("NOT source-set split") — also would require `AnalyticsClient` itself to differ per variant or a `@Binds` per source set, more moving parts than one runtime check.

### 5. `AnalyticsClient.register()` API stays as-is
`AnalyticsModule` calls the existing `fun register(provider: AnalyticsProvider)` — no change to `AnalyticsClient`'s constructor (e.g. no `Set<AnalyticsProvider>` multibinding). This is a direct user clarification and keeps `AnalyticsClientTest` (in `:core`, already passing) untouched. A Hilt `@IntoSet` multibinding for `AnalyticsProvider` was considered and explicitly rejected by the user in favour of the simpler, already-shipped `register()` call.

### 6. Graph-assembly verification: one real instrumented test, not just module unit tests
Module-level unit tests (Decision 3) prove each `@Provides` function's *logic* is correct in isolation, but they do not prove Hilt can actually wire `FakeStoreApplication → SingletonComponent → NetworkModule/AnalyticsModule/DataModule → MainActivity` without a missing binding, a scope mismatch, or a circular dependency — that can only be proven by letting Hilt's annotation processor and runtime actually assemble the graph. Add `HiltDependencyGraphTest` under `app/src/androidTest/`, annotated `@HiltAndroidTest` with `@get:Rule val hiltRule = HiltAndroidRule(this)`, injecting `@Inject lateinit var productRepository: ProductRepository` and asserting `productRepository is ProductRepositoryImpl`. This requires:
- `androidTestImplementation(libs.hilt.android.testing)` and `kspAndroidTest(libs.hilt.compiler)`,
- a custom `HiltTestRunner : AndroidJUnitRunner` overriding `newApplication(...)` to return `HiltTestApplication`, and
- `testInstrumentationRunner = "com.guidovezzoni.fakestore.HiltTestRunner"` in `app/build.gradle.kts` `defaultConfig` (replacing the current `androidx.test.runner.AndroidJUnitRunner`).

This test runs on-device/emulator per the project's existing Compose UI test convention (`connectedDebugAndroidTest`), consistent with how the project already gates "PASSED" verification on on-device checks, not just JVM unit tests.

*Alternative considered*: rely solely on `./gradlew assembleDebug` succeeding as proof the graph compiles. Rejected as insufficient — successful *compilation* proves Dagger/Hilt's static graph validation passed (no missing/duplicate bindings), but does not prove the *runtime* resolution of `ProductRepository → ProductRepositoryImpl` is correct, which is an explicit testing requirement in the story.

### 7. `FakeStoreApplication` and Kover exclusion
`FakeStoreApplication` contains no logic beyond the `@HiltAndroidApp` annotation (which generates a base class Hilt injects into) — it is added to `app/build.gradle.kts`'s Kover `classes` excludes list (`*.FakeStoreApplication`) alongside the existing `*.MainActivity` exclusion, for the same reason: an Android framework entry-point class with no business logic to unit-test.

## Risks / Trade-offs

- **[Risk]** Hilt/KSP version compatibility with AGP `9.3.1` / Kotlin `2.2.10` / compileSdk `37` is unverified against this exact combination (all recent, possibly ahead of what Hilt's release notes have explicitly certified). → **Mitigation**: pin `hiltVersion = "2.60"` in the version catalog and the KSP plugin to the release matching Kotlin `2.2.10` (KSP versions follow the `<kotlin-version>-<ksp-revision>` scheme, e.g. `2.2.10-2.0.2`); if `./gradlew assembleDebug` fails on annotation processing, the first task in `tasks.md` is to confirm/adjust the exact KSP patch version — this is called out as an Open Question below rather than guessed with false confidence.
- **[Risk]** `HiltDependencyGraphTest` requires a connected device/emulator (`connectedDebugAndroidTest`), consistent with the project's existing Compose UI test pattern, but means this specific acceptance criterion cannot be verified by `./gradlew test` alone. → **Mitigation**: documented explicitly in `tasks.md`'s verification section, following the project's "Verification Discipline" guideline (on-device checks are part of the gate, not optional).
- **[Trade-off]** Introducing `HiltTestRunner` and switching `testInstrumentationRunner` changes the default runner for *all* instrumented tests in `:app`, not just the new one — any future Compose UI test added to `app/src/androidTest/` will also run under `HiltTestApplication`. → Accepted: `HiltTestApplication` is a drop-in `Application` replacement (extends `MultiDexApplication`) and is the standard, recommended setup once any Hilt instrumented test exists in a module: see `dagger.dev/hilt/instrumentation-testing.html`.
- **[Trade-off]** `ApiService`, `ProductRepository`, `GetProductsUseCase` are unscoped (`@Provides` with no `@Singleton`), so Hilt constructs a fresh instance per injection site. → Accepted per Decision 3: these are stateless and cheap to construct; only the network client and analytics client have an explicit "singleton-scoped" acceptance criterion.

## Migration Plan

Additive within `:app`; `:core`'s only change is `NetworkClient` object → class (one mechanical test update, zero behavioural change). Sequencing in `tasks.md`: version catalog + Gradle plugin wiring first, then the `NetworkClient` object→class BDD pair (since `NetworkModule` depends on it), then `NetworkModule`/`AnalyticsModule`/`DataModule` BDD pairs (each independently testable), then the `FakeStoreApplication`/manifest/`MainActivity` integration wiring that ties the modules into a bootable graph, then the instrumented graph-assembly test, then final verification (`assembleDebug`, `test`, `detektDebug`, `koverVerify`, on-device `connectedDebugAndroidTest`).

## Open Questions

- Exact KSP patch version for Kotlin `2.2.10` (expected `2.2.10-2.0.2` or the nearest published revision) should be confirmed against `https://github.com/google/ksp/releases` at implementation time rather than assumed — flagged as the first prerequisite task rather than hard-coded here with false certainty.
- Whether Hilt `2.60` (current at time of writing per Context7-verified docs) has any known incompatibility with AGP `9.3.1`/compileSdk `37` should be confirmed by the first `./gradlew assembleDebug` run after wiring the plugins; if it fails, drop to the latest Hilt patch that explicitly lists AGP 9.x support in its release notes.
