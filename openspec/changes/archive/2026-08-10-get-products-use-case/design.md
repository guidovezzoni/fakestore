## Context

The FakeStore app is currently a single `:app` module (Kotlin 2.2, Compose, Detekt 2.x with `warningsAsErrors: true`, Kover 95% minimum bound). This story introduces the first multi-module split. There is no existing networking, JSON, or coroutine-test infrastructure in `gradle/libs.versions.toml`. Downstream stories (1.2.1, 1.2.2, 1.1.3) will build the UI and DI wiring on top of what this story establishes, so the module boundaries and conventions set here need to be right the first time — retrofitting them later would touch every module.

Constraints carried over from the codebase guidelines (`docs/guidelines/guidelines-android.md`) and confirmed clarifications:
- Convention plugins (`build-logic`) are required once the project goes multi-module — no duplicated inline Gradle config.
- `:domain` must be pure Kotlin JVM, zero Android/framework imports.
- Nested API concepts (rating) must remain a value object (`Rating`), not be flattened into `Product`.
- Networking stack: **Retrofit + kotlinx-serialization + OkHttp** (user-confirmed).
- Base URL exposed via a `:core` `BuildConfig` field (user-confirmed) — requires `buildFeatures { buildConfig = true }`.
- `:app` does **not** depend on `:domain`/`:data` in this story — that wiring is explicitly deferred to 1.1.3 (user-confirmed).
- `detekt.yml` has `warningsAsErrors: true` and `MagicNumber` active outside property declarations — literals (timeouts, base URL) must be extracted to named constants.

## Goals / Non-Goals

**Goals:**
- Stand up `build-logic` with two convention plugins (`fakestore.android.library`, `fakestore.kotlin.library`) that replicate `:app`'s existing detekt/kover/compile configuration so `:core`, `:domain`, `:data` don't duplicate it.
- Create `:core`, `:domain`, `:data` with the correct dependency direction: `:data` → `:domain` + `:core`; `:domain` has no dependencies on the other new modules.
- Deliver a working, fully-tested path: `ApiService` (Retrofit) → `ProductDto`/`RatingDto` → `ProductMapper` → `ProductRepositoryImpl` → `ProductRepository` → `GetProductsUseCase` → `Flow<Result<List<Product>>>`.
- Guarantee no unhandled exception escapes `GetProductsUseCase` — every failure path (timeout, no connectivity, malformed JSON, HTTP error) resolves to `Result.failure`.
- Reach 95%+ Kover coverage on all new code in `:domain` and `:data`.

**Non-Goals:**
- No UI, ViewModel, or Compose changes (story 1.2.1/1.2.2).
- No Hilt/manual DI wiring of the new modules into `:app` (story 1.1.3) — `:app`'s `build.gradle.kts` is untouched.
- No analytics instrumentation (story 1.1.2).
- No pagination, caching, or `Page<T>` wrapper — the API returns a flat list and the repository returns `List<Product>` directly (per story Assumption 4).
- No certificate pinning (noted as future consideration only).

## Decisions

### 1. Networking: Retrofit + OkHttp + kotlinx-serialization
User-confirmed. Retrofit's `kotlinx-serialization` converter (`retrofit2-kotlinx-serialization-converter`) avoids pulling in Moshi/Gson and keeps the JSON layer consistent with Kotlin's own serialization plugin, which is idiomatic for a Kotlin-first codebase. The `kotlinx.serialization.json.Json` instance is configured with `ignoreUnknownKeys = true` to satisfy the "API versioning" non-functional requirement (defensive against unexpected future fields).

*Alternative considered*: Moshi + codegen. Rejected — adds a second reflection/codegen toolchain for no benefit over kotlinx-serialization, which is already bundled with the Kotlin toolchain via the `kotlin.plugin.serialization` Gradle plugin.

### 2. Base URL via `:core` BuildConfig field
User-confirmed. `:core`'s `build.gradle.kts` sets `buildFeatures { buildConfig = true }` and defines `buildConfigField("String", "BASE_URL", "\"https://fakestoreapi.com\"")`. The Retrofit builder in `:core` reads `BuildConfig.BASE_URL` rather than a hardcoded literal, satisfying both the "Base URL hardcoding" security note and the `MagicNumber`/literal-extraction guideline. This also gives a future story a single point to override the URL per build variant (e.g. mock server for instrumented tests).

*Alternative considered*: A `NetworkConfig` object with a `const val` in `:core`. Rejected in favour of `BuildConfig` per explicit user clarification — `BuildConfig` is the more idiomatic Android mechanism for per-variant overrides.

### 3. Convention plugins replicate, not extend, `:app`'s current config
`AndroidLibraryConventionPlugin` and `KotlinLibraryConventionPlugin` are built by literally reading `:app`'s current `compileSdk`/`minSdk`/`targetSdk`, Java/Kotlin 11 compile options, and the existing `detekt { ... }` / `kover { ... }` blocks in `app/build.gradle.kts`, then lifting that configuration into the two plugin classes with zero behavioural change. `:app` itself is **not** migrated onto the convention plugin in this story (per the story's "Used by" note: "`:app` can migrate to it in a follow-up") — this avoids scope creep and keeps the diff to `:app` at zero for this story, consistent with the "`:app` → `:domain` deferred" clarification.

*Alternative considered*: Migrate `:app` onto `fakestore.android.library` (or a new `fakestore.android.application` plugin) now, for maximum DRY-ness immediately. Rejected — out of scope per the story text, and `:app` is a `com.android.application` module, not a `library`, so it would need its own convention plugin anyway; deferring keeps this story's surface area minimal.

### 4. Kover 95% bound applies per-module, scoped to new code
Each convention plugin configures the same Kover exclusion filters and `minBound(95)` as `:app` currently has. `:core` is expected to be mostly configuration/wiring code (Retrofit builder, OkHttp client) with little unit-testable logic; per Risk below, this is mitigated by keeping `:core` deliberately thin (a single network-config object) so the 95% bound is achievable without contrived tests, and by ensuring any conditional logic (e.g. timeout constants) is trivial enough not to need dedicated branch coverage.

### 5. Mapper visibility and error surfacing
`ProductMapper` lives in `:data` as an internal-visibility (or package-private-by-convention) function/object — not exposed to `:domain`, satisfying "not be accessible from `:domain`". `ProductRepositoryImpl.getProducts()` is a plain `suspend fun` that lets exceptions propagate (per the repository contract: "Throws on network/parsing failure"). `GetProductsUseCase` is the single place that catches everything via `flow { emit(Result.success(repository.getProducts())) }.catch { emit(Result.failure(it)) }`, guaranteeing exactly one emission per invocation and Flow completion, per the use-case contract's three testable behaviours (success, failure, completion).

### 6. Timeout constants
Connect/read timeouts (10–30s range per the story's NFR table) are extracted to named `private const val` constants in the `:core` network configuration file, satisfying both `detekt`'s `MagicNumber` rule and the "Development Best Practices — Literals extraction" guideline.

## Risks / Trade-offs

- **[Risk]** `:core`'s 95% Kover bound may be hard to hit if the module is pure Retrofit/OkHttp builder wiring with no branches. → **Mitigation**: Keep `:core` to a single object exposing a lazily-built `Retrofit` instance; if no testable logic exists beyond straight-line construction, Kover line coverage on straight-line code is still 100% once exercised by a single instantiation test — add one such test asserting the built `Retrofit` instance's `baseUrl()` matches `BuildConfig.BASE_URL`.
- **[Risk]** Convention plugin duplication drift — `:app` keeps its inline detekt/kover config while `build-logic` has a second copy for `:core`/`:data`/`:domain`, so a future detekt.yml change must be applied in two places until `:app` migrates. → **Mitigation**: Both configurations point at the same root `config/detekt/detekt.yml` file and the same `95` bound constant conceptually; document the duplication explicitly in code comments in both places, and note `:app` migration as a natural follow-up (already flagged in the story).
- **[Risk]** `warningsAsErrors: true` means any unused import or magic number in the new modules fails the build outright. → **Mitigation**: Run `./gradlew detektDebug` as part of the verification tasks before considering the story done; extract all literals (timeouts, base URL) to constants up front.
- **[Risk]** kotlinx-serialization requires the `kotlin("plugin.serialization")` Gradle plugin applied wherever `@Serializable` DTOs live (`:data`) — easy to forget since it's a separate plugin from `kotlinx-serialization-json` the library. → **Mitigation**: Explicitly add `alias(libs.plugins.kotlin.serialization)` (or apply directly) to `:data`'s `build.gradle.kts` as a tracked task; add to `libs.versions.toml` plugins block.
- **[Trade-off]** Returning `List<Product>` directly (no `Page<T>` wrapper) is simpler now but the original technical notes suggested pagination-readiness. → Accepted per story Assumption 4 — deferred until an API with pagination metadata exists; documented as a known simplification, not a regression.

## Migration Plan

Not applicable in the traditional sense (no data migration, no rollback of live systems) — this is new, additive code with zero existing consumers. Sequencing is captured in `tasks.md`: convention plugins and module scaffolding first, then domain/DTO models, then the BDD-paired mapper → repository → use-case implementation, then final detekt/kover/test verification. Because `:app` gains no new dependency in this story, the change can be merged without any behavioural change to the shipping app; the new modules are inert until story 1.1.3 wires them in.

## Open Questions

None outstanding — networking stack, base URL mechanism, and `:app` dependency scope were all resolved via user clarification before this design was written.
