## ADDED Requirements

### Requirement: User profile domain model
`:domain` SHALL define a pure Kotlin `UserProfile` data class with fields `id: Int`, `userName: String`, `name: UserName`, and `email: String`, with no Android, networking, or serialisation framework imports, and no `password`, `phone`, or `address` field.

#### Scenario: UserProfile preserves name as a value object
- **WHEN** a `UserProfile` instance is constructed
- **THEN** its `name` field is a `UserName` value object (not flattened `firstName`/`lastName` fields on `UserProfile` itself)

#### Scenario: UserProfile has no sensitive fields
- **WHEN** `UserProfile`'s declared properties are inspected
- **THEN** there is no property named `password`, `phone`, or `address`

### Requirement: UserName value object
`:domain` SHALL define a pure Kotlin `UserName` data class with fields `firstName: String` and `lastName: String`, representing the user's name as a coherent domain concept, displayed exactly as received with no capitalisation or formatting applied by any layer.

#### Scenario: UserName is constructed from first and last name
- **WHEN** a `UserName` instance is constructed with `firstName = "william"` and `lastName = "hopkins"`
- **THEN** `userName.firstName` equals `"william"` and `userName.lastName` equals `"hopkins"`, with casing unchanged

### Requirement: DTO models mirror only the consumed subset of the API response shape
`:data` SHALL define `UserDto` (fields `id`, `email`, `username`, and nested `name: UserNameDto`) and `UserNameDto` (fields `firstname`, `lastname`) as JSON-deserialisable data classes. Neither `UserDto` nor any related DTO SHALL declare a `password`, `phone`, `address`, or `__v` field. Deserialisation SHALL rely on `ignoreUnknownKeys = true` (already configured on the shared JSON configuration) so that these undeclared fields present in the raw API response are silently ignored rather than causing a parse failure.

#### Scenario: UserDto deserialises a well-formed API response, ignoring undeclared sensitive fields
- **WHEN** the JSON parser deserialises a user object from `GET https://fakestoreapi.com/users/8` whose raw response includes `password`, `phone`, `address`, and `__v`
- **THEN** deserialisation succeeds, the resulting `UserDto` has `id`, `email`, `username`, and a nested `UserNameDto` populated, and no exception is thrown due to the extra fields

#### Scenario: UserDto has no password property
- **WHEN** `UserDto::class.java.declaredFields` (or equivalent reflection) is inspected
- **THEN** no field named `password` exists

#### Scenario: UserDto has no phone, address, or __v property
- **WHEN** `UserDto::class.java.declaredFields` is inspected
- **THEN** no field named `phone`, `address`, or `__v` exists

### Requirement: UserDto to UserProfile mapping
A mapper in `:data` SHALL convert a `UserDto` (and its nested `UserNameDto`) into a `UserProfile` domain model (and its nested `UserName` value object), mapping `id`, `email`, `username` (to `UserProfile.userName`), and `name.firstname`/`name.lastname` (to `UserName.firstName`/`UserName.lastName`) without altering casing or applying any formatting, and SHALL NOT be accessible from `:domain`.

#### Scenario: Mapper converts all fields including nested name, preserving casing
- **GIVEN** a `UserDto` with `id = 8`, `email = "william@gmail.com"`, `username = "hopkins"`, and `name = UserNameDto(firstname = "william", lastname = "hopkins")`
- **WHEN** the mapper converts the DTO to a domain model
- **THEN** the resulting `UserProfile` has `id = 8`, `email = "william@gmail.com"`, `userName = "hopkins"`, and `name = UserName(firstName = "william", lastName = "hopkins")`, with no capitalisation applied

### Requirement: API service exposes a get-user-by-id endpoint
`:data` SHALL extend `ApiService` with `suspend fun getUser(@Path("id") id: Int): UserDto`, backed by the `:core` network client, targeting `GET /users/{id}`.

#### Scenario: ApiService fetches a single user by id
- **WHEN** `ApiService.getUser(id = 8)` is invoked against a successful `GET /users/8` response
- **THEN** it returns a `UserDto` matching the response body's consumed fields

### Requirement: User repository contract
`:domain` SHALL define a `UserRepository` interface with `suspend fun getUserProfile(id: Int): UserProfile`, returning domain models only, and throwing on network or parsing failure. `:data` SHALL provide `UserRepositoryImpl`, which calls `ApiService.getUser`, applies the mapper, and returns the mapped domain model.

#### Scenario: Repository returns a mapped domain model on success
- **GIVEN** `ApiService.getUser(id)` returns a `UserDto`
- **WHEN** `UserRepositoryImpl.getUserProfile(id)` is called
- **THEN** it returns a `UserProfile` that is the mapped equivalent of that DTO, and no `UserDto` or `UserNameDto` is exposed outside `:data`

#### Scenario: Repository propagates a network exception
- **GIVEN** `ApiService.getUser(id)` throws an `IOException`
- **WHEN** `UserRepositoryImpl.getUserProfile(id)` is called
- **THEN** the exception propagates unchanged out of `getUserProfile()`

### Requirement: GetUserProfileUseCase emits a single Result and completes
`:domain` SHALL define `GetUserProfileUseCase(private val repository: UserRepository)` with `operator fun invoke(id: Int): Flow<Result<UserProfile>>`, emitting exactly one `Result.success` containing the profile on success, exactly one `Result.failure` wrapping any caught exception on error, and completing the `Flow` after that single emission in either case.

#### Scenario: Use case emits success and completes
- **GIVEN** `UserRepository.getUserProfile(id)` returns a `UserProfile`
- **WHEN** `GetUserProfileUseCase()` is invoked with that `id` and collected
- **THEN** exactly one `Result.success` containing that profile is emitted, and the `Flow` completes with no further emissions

#### Scenario: Use case emits failure and completes on repository exception
- **GIVEN** `UserRepository.getUserProfile(id)` throws an exception (network error, timeout, or malformed JSON)
- **WHEN** `GetUserProfileUseCase()` is invoked and collected
- **THEN** exactly one `Result.failure` wrapping that exception is emitted, no exception escapes the `Flow` collector, and the `Flow` completes
