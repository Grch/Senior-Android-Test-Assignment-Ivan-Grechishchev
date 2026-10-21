## RENAMED Requirements

- FROM: `### Requirement: Quotes feature is split into api and impl modules`
- TO: `### Requirement: Quotes feature is consolidated in one module`
- FROM: `### Requirement: Quotes API exposes a minimal public entry contract`
- TO: `### Requirement: Quotes feature exposes a direct root composable`
- FROM: `### Requirement: Presentation uses Compose with MVVM ownership`
- TO: `### Requirement: Presentation uses Compose with MVVM ownership and preserves update flashes`

## MODIFIED Requirements

### Requirement: Quotes feature is consolidated in one module

The system MUST provide the quotes feature in exactly one Android library module, `:feature:quotes`, alongside `:app`. The single feature module MUST contain the complete quotes implementation, including Compose presentation, MVVM/ViewModel, networking, models, and mappers. The system MUST NOT include separate `:feature:quotes:api` or `:feature:quotes:impl` Gradle modules. No additional leaf Gradle module MUST be introduced.

#### Scenario: App depends on the consolidated feature module

- **WHEN** the module dependency graph is inspected
- **THEN** `:app` depends on the single `:feature:quotes` module containing the implementation
- **AND** no `:feature:quotes:api` or `:feature:quotes:impl` project exists

#### Scenario: Consumers use the direct feature module

- **WHEN** any consumer needs to integrate the quotes feature
- **THEN** it depends directly on `:feature:quotes`
- **AND** it does not depend on or compile against a separate quotes implementation module

#### Scenario: Exactly two Android leaf modules exist

- **WHEN** `settings.gradle.kts` and the included Gradle projects are inspected
- **THEN** the only leaf modules with build scripts are `:app` and `:feature:quotes`
- **AND** no additional module, including any generic `core` module, is declared

### Requirement: Quotes feature exposes a direct root composable

The `:feature:quotes` module MUST expose its screen through a public root composable or equivalent direct rendering entry. The system MUST NOT define a separate `QuotesEntryPoint` contract, a `QuotesDestination.ROUTE` identifier, a concrete API adapter, or app-side binding of such an adapter. The foundation MUST NOT introduce a dependency-injection or navigation framework.

#### Scenario: Feature exposes its root composable without a route contract

- **WHEN** the public surface of `:feature:quotes` is inspected
- **THEN** it exposes a direct root composable for rendering the quotes feature
- **AND** it does not declare `QuotesEntryPoint` or `QuotesDestination.ROUTE`

#### Scenario: Feature root renders the quotes screen directly

- **WHEN** the `:feature:quotes` module is inspected
- **THEN** the module's root composable renders the feature directly
- **AND** no concrete entry-point adapter or separate API module exists

#### Scenario: App renders the feature directly

- **WHEN** the `:app` composition root renders the quotes feature
- **THEN** it calls the feature root composable directly
- **AND** no API contract, adapter binding, dependency-injection framework, or navigation framework is required

### Requirement: App module is the composition root

The `:app` module MUST host the application entry screen and depend directly on the single `:feature:quotes` module. `MainActivity` MUST render the quotes root directly within the existing application theme, without constructing or binding a separate entry-point adapter. No other module may construct or wire the quotes feature on behalf of `:app`.

#### Scenario: App depends on the single feature module

- **WHEN** the module graph and app dependencies are inspected
- **THEN** `:app` depends on `:feature:quotes`
- **AND** it does not depend on or wire `:feature:quotes:api` and `:feature:quotes:impl`

#### Scenario: App launches the quotes feature

- **WHEN** the application is launched on Android
- **THEN** `MainActivity` presents `QuotesRoot()` directly inside the existing theme
- **AND** no separate contract, route, adapter, binding, or navigation framework is required

### Requirement: Presentation uses Compose with MVVM ownership and preserves update flashes

The Android presentation layer MUST use Jetpack Compose for rendering and an MVVM ViewModel for state ownership. The feature ViewModel MUST own screen state and orchestration, while Compose MUST only render state and dispatch actions. A socket or data stream MUST NOT be owned by a composable.

Directional price updates MUST retain the existing green-up/red-down row flash, using 300 ms as the documented default-duration assumption.

#### Scenario: Compose renders state and dispatches actions

- **WHEN** the quotes screen composables are inspected
- **THEN** the screen composable accepts state and an action callback and renders them
- **AND** the screen composable does not access a socket, repository, or data source directly

#### Scenario: ViewModel owns screen state

- **WHEN** the quotes screen is shown
- **THEN** its state is exposed by the feature ViewModel as an observable state flow
- **AND** user interactions are forwarded from Compose to the ViewModel as actions

#### Scenario: Directional price update uses the assumed flash duration

- **WHEN** an incoming update raises or lowers the last-trade price
- **THEN** the row flashes green for an upward move or red for a downward move
- **AND** the flash uses the assumed default duration of 300 ms

### Requirement: Quote transport and payload decoding are separated from presentation

The single `:feature:quotes` module MUST retain `presentation`, `domain`, and `data` package boundaries. The `domain` package MUST remain pure Kotlin and contain domain models plus repository/data-source abstractions only. The domain `QuoteRepository.observeQuotes(tickers)` contract MUST expose `Flow<Quote>`. `Quote`, domain interfaces, and `QuotesViewModel` MUST be free of OkHttp, Retrofit, Moshi, DTO, and data-implementation types. OkHttp, Retrofit, Moshi, DTOs, and their implementations MUST remain in `data`. The `QuotesViewModel` MUST depend only on the domain `QuoteRepository` and domain top-securities data-source abstraction; it MUST NOT import data, transport/serialization, DTO, or parsed wire-frame types. Compose MUST NOT handle socket or JSON behavior.

#### Scenario: ViewModel depends on domain contracts

- **WHEN** the ViewModel dependencies and imports are inspected
- **THEN** quotes are observed through the domain `QuoteRepository`
- **AND** top securities are fetched through a domain data-source abstraction
- **AND** no data-layer or transport/serialization dependency is imported by the ViewModel

#### Scenario: Raw socket frames are parsed and mapped by the data repository

- **WHEN** a WebSocket frame is received
- **THEN** `WebSocketRemoteDataSource` emits the raw frame unchanged without parsing or mapping it
- **AND** the data-layer quote repository uses Moshi DTO adapters to parse the raw frame's verified `q` array envelope and payload
- **AND** it maps valid partial payloads to pure domain `Quote` values
- **AND** `QuoteRepository.observeQuotes(tickers)` exposes those values as `Flow<Quote>`
- **AND** it ignores unrelated or malformed frames without adding or reordering active tickers

#### Scenario: Top-securities source abstraction remains in domain

- **WHEN** presentation requests top securities
- **THEN** it calls a domain-owned `TopSecuritiesDataSource` abstraction
- **AND** the Retrofit service and implementation remain in `data`

### Requirement: WebSocket remote data source owns transport lifecycle and reconnect

`WebSocketRemoteDataSource` MUST solely own OkHttp WebSocket opening, closing/cancellation, ping configuration, subscription sending, and reconnect. It MUST expose raw text frames as exactly `Flow<String>`, send the existing single-frame `quotes` subscription using the active ticker order, retain the 30-second ping, and retry a disconnect/failure after 2 seconds while collected. It MUST NOT parse JSON or map frames to `Quote`; those responsibilities belong to the data-layer repository. Cancelling collection MUST cancel the raw WebSocket and stop retrying immediately. The ViewModel MUST continue to gate collection on both app foreground and screen visibility and MUST NOT own socket reconnect delays.

#### Scenario: Collection opens the socket and sends its subscription

- **WHEN** a visible, foregrounded screen collects quote updates
- **THEN** the data source opens the existing no-auth WebSocket endpoint
- **AND** sends the existing `quotes` subscription as one text frame with the active tickers in order
- **AND** emits received text frames unchanged
- **AND** OkHttp retains its 30-second ping interval

#### Scenario: Data source reconnects after disconnect

- **WHEN** the socket fails or closes while collection remains active
- **THEN** the data source waits 2 seconds and opens/resubscribes a socket
- **AND** the ViewModel does not implement a reconnect loop or retry delay

#### Scenario: Hidden screen or backgrounded app cancels transport

- **WHEN** either screen visibility or app foreground state becomes false
- **THEN** the ViewModel cancels collection
- **AND** the source cancels the raw OkHttp WebSocket and does not reconnect

### Requirement: Inbound quote and top-securities JSON uses Moshi DTO adapters

Inbound WebSocket quote `q` frames and REST top-securities response bodies MUST be decoded using Square Moshi DTO adapters in the data layer. The inbound payload parsers MUST NOT use manual `org.json` traversal. The REST request MUST remain `GET /api/` with its existing serialized `q` parameter; the WebSocket subscription and `q` event schema MUST remain unchanged. The implementation MUST preserve partial quote fields and the API failure/empty fixed-list fallback.

#### Scenario: Quote frame uses the existing `q` envelope

- **WHEN** a valid quote frame is received as `[`q`, {named quote fields}, ...]`
- **THEN** a Moshi DTO adapter decodes the documented fields, including nullable partial fields
- **AND** the repository emits a domain quote with the ticker and only supplied values

#### Scenario: Top-securities response uses a Moshi DTO

- **WHEN** the existing REST endpoint returns a top-securities JSON body
- **THEN** a Moshi DTO adapter reads the ordered ticker list and optional ticker names
- **AND** API failure, malformed, or empty responses continue to select the complete fixed ticker list

### Requirement: Architecture refinement preserves screen-open ticker selection

This architecture refinement MUST preserve the existing screen-open behavior: the top-securities API is requested by default whenever the quotes screen opens; a non-empty response selects the API tickers in response order, and any API failure or empty response selects the complete fixed ticker list in its original order. The behavior MUST NOT require user action or introduce a toggle, feature flag, build switch, or persisted source preference.

#### Scenario: Screen open requests the API and resolves the complete active set

- **WHEN** the quotes screen opens
- **THEN** the top-securities API is requested by default without user action
- **AND** a non-empty response replaces the active set with the API tickers in response order
- **AND** an API failure or empty response selects the complete fixed ticker list in its original order
- **AND** the resolved active ticker set continues to drive the existing live quote subscription

### Requirement: This change adds no test sources or test coverage

This change MUST NOT add test sources or test coverage to any module, and it MUST NOT remove existing tests.

#### Scenario: No test sources are added

- **WHEN** the source sets of every module are inspected
- **THEN** no test source directory or test source file is added by this change
- **AND** existing test sources remain in place

### Requirement: Foundation remains Android-only and minimal

The foundation MUST target Android only. It MUST NOT introduce an iOS target or Kotlin Multiplatform targets, and MUST NOT create speculative generic `core` modules before a second consumer exists.

#### Scenario: No iOS target exists

- **WHEN** the Gradle configuration and source sets are inspected
- **THEN** only Android application and Android library targets are present
- **AND** no iOS or Kotlin Multiplatform target is declared

#### Scenario: No speculative core module

- **WHEN** the module list is inspected
- **THEN** no generic `core` module is added solely in anticipation of future sharing
- **AND** shared code is placed in a new module only when at least two features consume it
