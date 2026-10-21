## MODIFIED Requirements

### Requirement: Quotes feature is consolidated in one module

The system MUST provide the quotes feature as exactly two actual Gradle modules, `:feature:quotes:api` and `:feature:quotes:impl`, alongside `:app`. The parent `feature/quotes/` directory MUST NOT declare a Gradle build or contain source code. `:feature:quotes:impl` MUST depend on `:feature:quotes:api`; `:feature:quotes:api` MUST NOT depend on `:feature:quotes:impl` or `:app`. The API MUST remain framework-free Kotlin/JVM and the implementation MUST be an Android library. No generic core module or other leaf module is introduced.

#### Scenario: App depends on the consolidated feature module

- **WHEN** the Gradle project graph and quotes module directories are inspected
- **THEN** `:app`, `:feature:quotes:api`, and `:feature:quotes:impl` are the leaf projects
- **AND** `feature/quotes/` has no build script or source content at its parent level
- **AND** the implementation depends on the API, not vice versa

#### Scenario: Consumers use the direct feature module

- **WHEN** any consumer needs to integrate the quotes feature
- **THEN** it depends on `:feature:quotes:impl` for direct root integration or on `:feature:quotes:api` for domain contracts only
- **AND** it does not depend on the source-free parent project

#### Scenario: Exactly two Android leaf modules exist

- **WHEN** `settings.gradle.kts` and the included Gradle projects are inspected
- **THEN** the only feature leaf modules are `:feature:quotes:api` and `:feature:quotes:impl` alongside `:app`
- **AND** no additional module, including any generic `core` module, is declared

#### Scenario: API is framework-free

- **WHEN** the API module's source and dependencies are inspected
- **THEN** its public contracts and domain types use only Kotlin/JVM and required coroutine APIs
- **AND** it contains no Android, Compose, Hilt, OkHttp, Retrofit, or Moshi types

### Requirement: Quotes feature exposes a direct root composable

The `:feature:quotes:impl` module MUST expose a public `QuotesRoot()` composable for direct rendering. It MUST NOT declare a `QuotesEntryPoint` contract, `QuotesDestination.ROUTE`, a concrete entry-point adapter, or a navigation framework.

#### Scenario: Feature exposes its root composable without a route contract

- **WHEN** the public surface of `:feature:quotes:impl` is inspected
- **THEN** it exposes `QuotesRoot()` for rendering the quotes feature
- **AND** it does not declare `QuotesEntryPoint` or `QuotesDestination.ROUTE`

#### Scenario: Feature root renders the quotes screen directly

- **WHEN** the quotes implementation is rendered
- **THEN** its root composable renders the quotes screen directly using its ViewModel
- **AND** no navigation framework or app-side entry-point adapter is required

#### Scenario: App renders the feature directly

- **WHEN** the `:app` composition root renders the quotes feature
- **THEN** it calls the impl module's root composable directly
- **AND** no API contract, adapter binding, or navigation framework is required

### Requirement: App module is the composition root

The `:app` module MUST host the application entry screen and depend directly on `:feature:quotes:impl`. `MainActivity` MUST render `QuotesRoot()` directly within the existing application theme. The app MUST initialize Hilt and `MainActivity` MUST be an Android entry point; the app MAY remain unaware of the quotes implementation's internal dependencies.

#### Scenario: App depends on the single feature module

- **WHEN** the module graph and app dependencies are inspected
- **THEN** `:app` depends on `:feature:quotes:impl`
- **AND** it does not need a direct API dependency for entry-point binding

#### Scenario: App launches the quotes feature

- **WHEN** the Android application launches
- **THEN** its Hilt application component is initialized and `MainActivity` is an Android entry point
- **AND** `MainActivity` calls `QuotesRoot()` directly inside the existing theme
- **AND** no separate route, adapter, or navigation framework is required

### Requirement: Quote transport and payload decoding are separated from presentation

The quotes feature MUST retain API/domain contracts separate from Android presentation and data implementations. The `:feature:quotes:api` module MUST contain pure Kotlin domain models and contracts, including `Quote`, `QuoteRepository.observeQuotes(tickers): Flow<Quote>`, `TopSecuritiesDataSource`, top-securities result/error contracts, and fixed tickers as appropriate. The domain contracts MUST be free of Android, Compose, Hilt, OkHttp, Retrofit, Moshi, DTO, and data-implementation types. The `:feature:quotes:impl` module MUST contain the Compose/MVVM presentation, Retrofit, OkHttp, Moshi DTOs/adapters, data sources, repositories, and mappings. `QuotesViewModel` MUST depend only on API contracts; Compose MUST NOT handle socket or JSON behavior.

#### Scenario: ViewModel depends on domain contracts

- **WHEN** the ViewModel dependencies and imports are inspected
- **THEN** quotes are observed through API `QuoteRepository`
- **AND** top securities are fetched through API `TopSecuritiesDataSource`
- **AND** no data-layer or transport/serialization dependency is imported by the ViewModel

#### Scenario: Raw socket frames are parsed and mapped by the data repository

- **WHEN** a WebSocket frame is received
- **THEN** the raw WebSocket source emits the raw frame unchanged
- **AND** the implementation repository uses Moshi DTO adapters to decode quote frames and map valid partial payloads to domain `Quote` values
- **AND** it ignores unrelated or malformed frames without adding or reordering active tickers

#### Scenario: Top-securities source abstraction remains in domain

- **WHEN** presentation requests top securities
- **THEN** it calls the API-owned `TopSecuritiesDataSource` abstraction
- **AND** the Retrofit service and implementation remain in `impl`

### Requirement: Hilt owns Android dependency wiring

The Android application and quotes implementation MUST use generated Hilt dependency injection. `:app` MUST provide the Hilt application root, and `:feature:quotes:impl` MUST provide Hilt's ViewModel and data graph. The API module MUST remain independent of Hilt.

#### Scenario: Hilt resolves the quotes ViewModel and repository contracts

- **WHEN** `QuotesRoot()` requests its ViewModel
- **THEN** Hilt provides the `@HiltViewModel` using constructor injection
- **AND** the quote repository and top-securities data-source API contracts resolve to their implementation bindings

#### Scenario: Hilt transport dependencies are configured once

- **WHEN** the implementation's data graph is created
- **THEN** Hilt provides the configured OkHttp client with the existing 30-second ping, Retrofit service/client, Moshi and adapters
- **AND** implementation bindings are installed in the narrowest applicable Hilt component
- **AND** KSP-generated Hilt code participates in a successful Gradle compile

### Requirement: Existing live quotes behavior is preserved

The module and dependency-injection change MUST preserve the existing Tradernet behavior: unauthenticated WebSocket streaming, raw-frame transport ownership, 30-second ping, 2-second reconnect while collected, foreground-and-visible collection gating, screen-open top-securities request, complete fixed-list fallback, active ticker order/resubscription, existing REST and WebSocket wire formats, quote row presentation/colors, and 300 ms directional flash. No test sources or test coverage are added and existing tests remain untouched.

#### Scenario: Live collection and subscription lifecycle remain unchanged

- **WHEN** the screen is visible and the app is foregrounded
- **THEN** the current active ticker set is subscribed in order without authentication
- **AND** socket ping/reconnect and cancellation retain the existing behavior
- **AND** collection stops when either foreground or visibility gating becomes false

#### Scenario: Screen-open top securities and display remain unchanged

- **WHEN** the quotes screen opens
- **THEN** the top-securities GET is requested and non-empty results apply in response order
- **AND** an error or empty result selects the complete fixed ticker list
- **AND** rows continue to render existing data, colors, and directional flashes for 300 ms

#### Scenario: No tests are added or removed

- **WHEN** module source sets and existing app test sources are inspected
- **THEN** this change adds or modifies no test source
- **AND** existing app tests remain untouched
