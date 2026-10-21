## Purpose

Defines the Gradle module topology, dependency direction, composition-root wiring, and Compose/MVVM presentation contract for the Android application so that features can be added as isolated modules behind a stable public boundary.

## ADDED Requirements

### Requirement: Quotes feature is split into api and impl modules

The system MUST provide a quotes feature split into a public `:feature:quotes:api` module and an implementation `:feature:quotes:impl` module. `:feature:quotes:impl` MUST depend on `:feature:quotes:api` rather than the reverse, and `:feature:quotes:api` MUST NOT depend on `:feature:quotes:impl` or on `:app`. Every consumer except the `:app` composition root MUST integrate through `:feature:quotes:api` only.

The foundation MUST consist of exactly three leaf Gradle modules (projects that declare a build script): `:app`, `:feature:quotes:api`, and `:feature:quotes:impl`. No additional leaf module MUST be introduced by this change.

#### Scenario: Implementation depends on API

- **WHEN** the module dependency graph is inspected
- **THEN** `:feature:quotes:impl` declares a dependency on `:feature:quotes:api`
- **AND** `:feature:quotes:api` declares no dependency on `:feature:quotes:impl` or on `:app`

#### Scenario: Consumers cannot see implementation

- **WHEN** any consumer other than the `:app` composition root needs to integrate the quotes feature
- **THEN** it depends only on `:feature:quotes:api`
- **AND** it does not compile against `:feature:quotes:impl` types

#### Scenario: Exactly three foundation modules exist

- **WHEN** `settings.gradle.kts` and the included Gradle projects are inspected
- **THEN** the only leaf modules with build scripts are `:app`, `:feature:quotes:api`, and `:feature:quotes:impl`
- **AND** no additional module, including any generic `core` module, is declared

### Requirement: Quotes API exposes a minimal public entry contract

The `:feature:quotes:api` module MUST expose the quotes feature through a minimal public contract. It MUST declare a `QuotesEntryPoint` functional interface whose composable `Content(modifier: Modifier)` renders the feature, and it MUST declare `QuotesDestination.ROUTE` as the stable destination identifier. `:feature:quotes:impl` MUST provide the concrete implementation of `QuotesEntryPoint`. The `:app` composition root MUST bind that implementation to the contract and present the feature through it. The foundation MUST NOT introduce a navigation framework.

#### Scenario: API exposes the entry point and route

- **WHEN** the public surface of `:feature:quotes:api` is inspected
- **THEN** it declares `QuotesEntryPoint` with a composable `Content(modifier: Modifier)` function
- **AND** it declares `QuotesDestination.ROUTE` as a stable destination identifier

#### Scenario: Impl provides the entry point implementation

- **WHEN** `:feature:quotes:impl` is inspected
- **THEN** it provides the concrete implementation of `QuotesEntryPoint`
- **AND** it resolves the contract from `:feature:quotes:api`

#### Scenario: App presents the feature through the contract

- **WHEN** the `:app` composition root renders the quotes feature
- **THEN** it supplies the concrete implementation to a `QuotesEntryPoint` typed value and calls `Content(modifier)`
- **AND** no navigation framework is introduced

### Requirement: App module is the composition root

The `:app` module MUST act as the composition root. As the composition-root exception, `:app` is allowed to depend on both `:feature:quotes:api` and `:feature:quotes:impl` for assembly and wiring, and it is the only module permitted to depend on `:feature:quotes:impl`. It MUST be the only module that wires the quotes feature implementation to its public API entry point, and it MUST host the application entry screen that presents the quotes feature.

#### Scenario: App wires api to impl

- **WHEN** the module graph and dependency wiring are inspected
- **THEN** `:app` depends on both `:feature:quotes:api` and `:feature:quotes:impl`
- **AND** the concrete quotes implementation is provided through the `:feature:quotes:api` boundary

#### Scenario: App launches the quotes feature

- **WHEN** the application is launched on Android
- **THEN** the app entry point presents the quotes feature exposed through `:feature:quotes:api`
- **AND** no navigation framework is required to present it
- **AND** no other module directly constructs the quotes implementation

### Requirement: Presentation uses Compose with MVVM ownership

The Android presentation layer MUST use Jetpack Compose for rendering and an MVVM ViewModel for state ownership. The feature ViewModel MUST own screen state and orchestration, while Compose MUST only render state and dispatch actions. A socket or data stream MUST NOT be owned by a composable.

#### Scenario: Compose renders state and dispatches actions

- **WHEN** the quotes screen composables are inspected
- **THEN** the screen composable accepts state and an action callback and renders them
- **AND** the screen composable does not access a socket, repository, or data source directly

#### Scenario: ViewModel owns screen state

- **WHEN** the quotes screen is shown
- **THEN** its state is exposed by the feature ViewModel as an observable state flow
- **AND** user interactions are forwarded from Compose to the ViewModel as actions

### Requirement: This change adds no test sources or test coverage

This change MUST NOT add test sources or test coverage to any module, and it MUST NOT remove existing tests.

#### Scenario: No test sources are added

- **WHEN** the source sets of every module are inspected
- **THEN** no test source directory or test source file is added by this change
- **AND** existing test sources remain in place

### Requirement: Foundation remains Android-only and minimal

The foundation MUST target Android only. It MUST NOT introduce an iOS target or Kotlin Multiplatform targets, and it MUST NOT create speculative generic `core` modules before a second consumer exists.

#### Scenario: No iOS target exists

- **WHEN** the Gradle configuration and source sets are inspected
- **THEN** only Android application and Android library targets are present
- **AND** no iOS or Kotlin Multiplatform target is declared

#### Scenario: No speculative core module

- **WHEN** the module list is inspected
- **THEN** no generic `core` module is added solely in anticipation of future sharing
- **AND** shared code is placed in a new module only when at least two features consume it
