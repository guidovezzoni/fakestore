# Intro

As an agent please do NOT modify the "General Introduction" section as that is meant to be updated manually by a human. However the following section, "Fakestore" can be reviewed and updated as required.

## General Introduction
In implementing this assessment I used an AI supported SDD methodology (Spec-Driven Development) in which, the specification are fully defined before starting coding, to force the agent to follow the established plan.
On top of the SDD library (OpenSpec) I used a framework I am developing, which, by using AI, allows to automate the full lifecycle, from user story refinement, to enforcing BDD, to final verification of the user story, including definition of done, unit tests, UI test, security assessment, on-device testing, etc.

The app has been treated as a production app, so technical and architectural decisions have been taken thinking this app as the first step for a larger app, although some assumptions and limitations exist.

### Assumptions
1. Currently, the user selection is not possible, the current user is hardcoded in order to reduce the scope of the project and fit the deadline.

### Limitations
1. The current REST API does not support pagination, it should be added as  it currently limits the app scalability. The app implementation is based on the current API structure and the implied assumption that the number of products is limited (currently 20). Accordingly, the combination of products and favourites is based on the same assumption. All this structure will have to be re-viewed once pagination is made available.
2. The Fake Store API returns `password`, `phone`, `address`, and `__v` in the user endpoint. The app mitigates this by declaring only the required fields in `UserDto` (relying on `ignoreUnknownKeys = true`), so sensitive fields never enter application memory.

### Product-level Clarifications
1. Requirements specify persistence for favourites but not for products, requirements do not mention caching or offline first approach. Accepted but should be clarified with Product.
2. Currency for product prices is hardcoded to USD. Ideally the price should come with a currency field.

### Technical Known Improvements
1. kover currently excludes also by annotatedBy, however the annotatedBy filter excludes the entire annotated class, excluding code that genuinely might need unit test, this needs to be reviewed and replaced by a more accurate exclusion.
2. Analytics events are currently implemented in a rough way - error conditions are not logged, events are strings, parameters usage can be tailored further.

## Additional info

### Github / process
The initial AI and project setup were committed directly on main, after that I created PR for each user story to simulate a way of working. PRs were merged and squashed on Github.
Each PR contains the code for its user story but also the documentation: user story, SDD artifacts, LLM reports.
PRs are fairly big, this is a tradeoff due to the size of the project and the time available, in normal situations PRs should be sized in such a way that can be easily reviewed by peers.

### AI Setup
The AI setup in the project is layered across different levels, but all are included in git, so they can be shared across different members of the team.
- AGENTS.md provides a general overview of the project. Also, the first part instructs the agent how to selectively find specific instructions for Android, git, user stories, etc. These parts are located in `docs/guidelines` and will be loaded by the agent when required.
- OpenSpec (https://github.com/Fission-AI/OpenSpec/) is used for handling the SDD processes, the commands used are: explore, propose, apply, verify, archive
- An additional library (SDLC), which I am currently developing, is handling the full lifecycle of user stories. More info at [SDLC-README.md](docs/sdlc/commands/SDLC-README.md). Commands are:
    - **/sdlc_open_story** analyses the next story to open, creates a branch, sets the story open and refines it adding a full and detailed product analysis
    - **/sdlc_propose_change** analyses the user story, asks for questions if something isn't clear, and finally generates the SDD artifacts: proposal, design, specs, and tasks. Tasks are defined with a BDD approach, based on acceptance criteria and test-first.
    - **/sdlc_implement_change** implements the current OpenSpec change using BDD Red/Green cycle (test tasks verified RED before implementation, implementation tasks verified GREEN after). Then runs a security review and updates the documentation.
    - **/sdlc_verify_story** this is an end-to-end verification gate before the story is archived and considered done. Runs OpenSpec's verify, runs a security review, checks all acceptance criteria. Finally it closes the story and archives the SDD artifacts.
    
The SDLC library is still work in progress, and I'm improving it while I use it, and adding additional features like LLM independence, self-improvement by adding learnt lessons.

# FakeStore

A native Android app built with Kotlin and Jetpack Compose that consumes the [Fake Store API](https://fakestoreapi.com/) to browse and display e-commerce product data.

## Tech Stack

- **Kotlin 2.2** with Jetpack Compose and Material 3
- **MVI architecture** (Model-View-Intent) with Clean Architecture layers
- **Navigation**: Jetpack Navigation Compose 2.9.8 with type-safe (`@Serializable`) routes and a 3-tab bottom navigation bar
- **Multi-module**: `build-logic` (convention plugins), `:core`, `:domain`, `:data`, `:app`
- **Networking**: Retrofit + OkHttp + kotlinx-serialization
- **Local Persistence**: Room 2.7 with KSP annotation processing; favourites stored in `FavouritesDatabase`
- **Image Loading**: Coil 3 (with OkHttp network engine)
- **Dependency Injection**: Hilt (Dagger-Hilt 2.60) + KSP; modules in `:app/di/`
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

# Run Compose tests
./gradlew connectedDebugAndroidTest
```

## Project Structure

The project uses a multi-module Clean Architecture with four Gradle modules:

- **build-logic/** — Convention plugins (`fakestore.android.library`, `fakestore.kotlin.library`) centralising Detekt, Kover, and compile configuration
- **:core** — Network client (Retrofit + OkHttp), `BuildConfig.BASE_URL`; analytics abstraction (`AnalyticsProvider` interface, `AnalyticsClient` dispatcher, `DebugAnalyticsProvider`)
- **:domain** — Domain models (`Product`, `Rating`, `UserProfile`, `UserName`), repository interfaces (`ProductRepository`, `FavouritesRepository`, `UserRepository`), use cases (`GetProductsUseCase`, `GetFavouriteIdsUseCase`, `ToggleFavouriteUseCase`, `GetUserProfileUseCase`) — pure Kotlin, no Android dependencies
- **:data** — DTOs, mappers, `ApiService`, `ProductRepositoryImpl`, `UserRepositoryImpl`; Room database (`FavouritesDatabase`, `FavouriteDao`, `FavouriteEntity`, `FavouritesRepositoryImpl`) — KSP annotation processing
- **:app** — Jetpack Compose UI, ViewModels (MVI), theme, navigation; Hilt DI modules (`NetworkModule`, `AnalyticsModule`, `DataModule`, `DatabaseModule`), `FakeStoreApplication` (`@HiltAndroidApp`)
