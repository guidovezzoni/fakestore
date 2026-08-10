## ADDED Requirements

### Requirement: Application-level Hilt graph bootstrap
`:app` SHALL define `FakeStoreApplication`, an `Application` subclass annotated `@HiltAndroidApp`, registered as the manifest application class via `android:name` in `AndroidManifest.xml`. The Hilt dependency graph SHALL be initialised when the application process starts.

#### Scenario: App process starts with the Hilt graph initialised
- **GIVEN** the FakeStore app is launched
- **WHEN** `FakeStoreApplication.onCreate()` runs
- **THEN** the generated Hilt `SingletonComponent` is available for injection into any `@AndroidEntryPoint` Android component in the app

### Requirement: MainActivity is a Hilt injection entry point
`MainActivity` SHALL be annotated `@AndroidEntryPoint`, making it eligible to receive field or constructor injection from the Hilt graph.

#### Scenario: MainActivity is annotated as an entry point
- **WHEN** `MainActivity`'s class declaration is inspected
- **THEN** it carries the `@AndroidEntryPoint` annotation and its `Application` (`FakeStoreApplication`) carries `@HiltAndroidApp`

### Requirement: NetworkClient is singleton-scoped and injectable
`:app` SHALL provide a `@Module @InstallIn(SingletonComponent::class)` `NetworkModule` with `@Provides @Singleton fun provideNetworkClient(): NetworkClient` and `@Provides @Singleton fun provideRetrofit(networkClient: NetworkClient): Retrofit`. `:core`'s `NetworkClient` SHALL be a plain class (not a Kotlin `object`), instantiable by the `@Provides` function, with its internal OkHttp/Retrofit construction logic (base URL from `BuildConfig.BASE_URL`, explicit connect/read timeouts) unchanged from its pre-DI form.

#### Scenario: NetworkClient resolves to a single shared instance
- **GIVEN** the Hilt `SingletonComponent` graph is assembled
- **WHEN** `NetworkClient` is injected at two different injection sites within the same application process
- **THEN** both sites receive the same `NetworkClient` instance

#### Scenario: Provisioned Retrofit is correctly configured
- **GIVEN** `NetworkModule.provideNetworkClient()` and `NetworkModule.provideRetrofit(networkClient)` are called directly, without booting the Hilt graph
- **WHEN** the resulting `Retrofit` instance's base URL is inspected
- **THEN** it equals `:core`'s `BuildConfig.BASE_URL` (`https://fakestoreapi.com/`)

### Requirement: AnalyticsClient is singleton-scoped and injectable
`:app` SHALL provide a `@Module @InstallIn(SingletonComponent::class)` `AnalyticsModule` with `@Provides @Singleton fun provideAnalyticsClient(): AnalyticsClient`, constructing `AnalyticsClient` and conditionally registering `DebugAnalyticsProvider` via the existing `AnalyticsClient.register(provider: AnalyticsProvider)` method.

#### Scenario: AnalyticsClient resolves to a single shared instance
- **GIVEN** the Hilt `SingletonComponent` graph is assembled
- **WHEN** `AnalyticsClient` is injected at two different injection sites within the same application process
- **THEN** both sites receive the same `AnalyticsClient` instance

### Requirement: Debug analytics provider registration is gated by build type at runtime
The `AnalyticsClient` provisioning logic SHALL register `DebugAnalyticsProvider` when `BuildConfig.DEBUG` is `true` and SHALL NOT register it when `BuildConfig.DEBUG` is `false`, implemented as a single runtime check inside one Hilt module (not via separate `debug`/`release` source sets). The gating logic SHALL be extracted into a function independently callable with an explicit `isDebug: Boolean` parameter, so both branches are unit-testable without booting the Hilt graph or building separate app variants.

#### Scenario: Debug provider is registered when BuildConfig.DEBUG is true
- **GIVEN** the analytics provisioning function is invoked with `isDebug = true`
- **WHEN** the resulting `AnalyticsClient.logEvent(name, params)` is called
- **THEN** a registered `DebugAnalyticsProvider` receives the event

#### Scenario: Debug provider is not registered when BuildConfig.DEBUG is false
- **GIVEN** the analytics provisioning function is invoked with `isDebug = false`
- **WHEN** the resulting `AnalyticsClient.logEvent(name, params)` is called
- **THEN** no `DebugAnalyticsProvider` is registered and no debug-only side effect occurs

#### Scenario: Release build variant does not register the debug provider
- **GIVEN** the app is compiled with the `release` build type, where `BuildConfig.DEBUG` is `false`
- **WHEN** the Hilt graph provisions `AnalyticsClient`
- **THEN** `DebugAnalyticsProvider` is not among its registered providers

### Requirement: ProductRepository resolves to ProductRepositoryImpl via injection
`:app` SHALL provide a `@Module @InstallIn(SingletonComponent::class)` `DataModule` with `@Provides fun provideProductRepository(apiService: ApiService): ProductRepository` returning a `ProductRepositoryImpl` instance. No `@Inject` annotation SHALL be added to `ProductRepositoryImpl`, `ProductRepository`, `ApiService`, or `GetProductsUseCase` — all provisioning logic lives exclusively in `:app`'s `di/` modules.

#### Scenario: Injected ProductRepository is a ProductRepositoryImpl
- **GIVEN** the Hilt `SingletonComponent` graph is assembled with a real or fake `ApiService` binding
- **WHEN** `ProductRepository` is injected
- **THEN** the resolved instance is of type `ProductRepositoryImpl`

#### Scenario: DataModule provisions ProductRepository without booting Hilt
- **GIVEN** `DataModule.provideProductRepository(apiService)` is called directly with a test double `ApiService`
- **WHEN** the return value's type is inspected
- **THEN** it is `ProductRepositoryImpl`

### Requirement: ApiService and GetProductsUseCase are injectable
`DataModule` SHALL provide `@Provides fun provideApiService(retrofit: Retrofit): ApiService` (via `retrofit.create(ApiService::class.java)`) and `@Provides fun provideGetProductsUseCase(repository: ProductRepository): GetProductsUseCase`.

#### Scenario: GetProductsUseCase is constructed from the provisioned repository
- **GIVEN** `DataModule.provideGetProductsUseCase(repository)` is called directly with a test double `ProductRepository`
- **WHEN** the returned `GetProductsUseCase` is invoked and collected
- **THEN** it delegates to the given `repository`'s `getProducts()`, consistent with `GetProductsUseCase`'s existing (unmodified) behaviour

### Requirement: :domain, :core, :data carry no Hilt/Android DI framework references
No `@Inject`, `@Module`, `@InstallIn`, `@Provides`, `@Binds`, `@HiltAndroidApp`, `@AndroidEntryPoint`, or `javax.inject.*`/`dagger.hilt.*` import SHALL appear anywhere in `:domain`, `:core`, or `:data` source. Hilt and KSP Gradle plugins SHALL be applied only in `app/build.gradle.kts`.

#### Scenario: Domain module has no DI framework dependency
- **WHEN** `domain/build.gradle.kts` and every `.kt` file under `domain/src/main/` are inspected
- **THEN** no Hilt or KSP plugin is applied and no Hilt/`javax.inject` import is present

#### Scenario: Core and data modules have no DI framework dependency
- **WHEN** `core/build.gradle.kts`, `data/build.gradle.kts`, and every `.kt` file under `core/src/main/` and `data/src/main/` are inspected
- **THEN** no Hilt or KSP plugin is applied and no Hilt/`javax.inject` import is present

### Requirement: Dependency graph assembles and resolves end-to-end
The full Hilt dependency graph (`FakeStoreApplication` → `SingletonComponent` → `NetworkModule` + `AnalyticsModule` + `DataModule`) SHALL assemble without a missing, duplicate, or circular binding, verifiable by an instrumented test that boots the real graph and injects `ProductRepository`.

#### Scenario: Graph assembly test resolves ProductRepository to ProductRepositoryImpl
- **GIVEN** a `@HiltAndroidTest`-annotated instrumented test running under `HiltTestApplication`
- **WHEN** `ProductRepository` is injected via `@Inject lateinit var productRepository: ProductRepository`
- **THEN** `productRepository` is non-null and is an instance of `ProductRepositoryImpl`

### Requirement: No manual construction of graph-managed types in application code
Once provisioned by Hilt, `NetworkClient`, `AnalyticsClient`, `Retrofit`, `ApiService`, `ProductRepository`, and `GetProductsUseCase` SHALL NOT be manually constructed (via `NetworkClient()`, `AnalyticsClient()`, `ProductRepositoryImpl(...)`, etc.) anywhere in `app/src/main/` outside the `di/` module files themselves.

#### Scenario: No manual instantiation outside di modules
- **WHEN** `app/src/main/java/com/guidovezzoni/fakestore/` (excluding `di/`) is inspected for direct constructor calls to `NetworkClient`, `AnalyticsClient`, or `ProductRepositoryImpl`
- **THEN** no such call exists — all resolution happens through Hilt injection
