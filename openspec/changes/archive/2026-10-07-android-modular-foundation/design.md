## Context

See proposal.md — Why. The repository is currently the Android Studio template: one `:app` module using the version catalog at `gradle/libs.versions.toml`, Compose enabled via the `kotlin.compose` plugin, and `settings.gradle.kts` including only `:app`. There are no existing specs under `openspec/specs/` and no established `core` modules. The assignment mandates Android-only, Compose + MVVM, and a pragmatic multi-module layout.

## Goals / Non-Goals

**Goals:**
- Provide exactly the modules needed for the quotes feature now: `:app`, `:feature:quotes:api`, `:feature:quotes:impl`.
- Keep a single, stable integration boundary so later optional changes (top-securities source, logos, min-step formatting) can be added inside `:feature:quotes:impl` without changing `:app`.
- Enforce dependency direction so implementation types cannot leak to consumers, with `:app` as the sole assembly exception that depends on both `api` and `impl`.
- Establish the Compose + MVVM contract that later quotes work must follow.

**Non-Goals:**
- Implementing the live quotes board, WebSocket, REST data sources, logos, or min-step formatting (separate changes).
- Introducing Kotlin Multiplatform or iOS targets.
- Creating generic `core:*` modules, a design system module, or shared navigation infrastructure before a second consumer exists.
- Authoring tests or test coverage, or removing existing tests.

## Decisions

- **Two-module feature instead of a three-layer feature.** The assignment asks for `:feature:quotes:api` and `:feature:quotes:impl`, which is a lighter split than domain/data/presentation modules. This keeps the feature cohesive while still exposing a public boundary. Alternative considered: separate domain/data/presentation modules per the generic skill template — rejected as over-engineering for a single screen and not what the assignment specifies.
- **Exact three-module topology.** The change adds exactly `:feature:quotes:api` and `:feature:quotes:impl` beside the existing `:app`, for a total of three modules; no other module, including any generic `core:*`, is created by this change.
- **`:app` is the only composition root and the sole dependency exception.** Wiring the concrete implementation to the public API in `:app` preserves the boundary and avoids service-locator coupling inside features. `:app` therefore depends on both `:feature:quotes:api` and `:feature:quotes:impl`; every other consumer depends on `:feature:quotes:api` only. Alternative considered: a shared DI module providing both — rejected as a speculative module.
- **`api` holds the public contract only.** It exposes `QuotesEntryPoint` with a composable `Content(modifier: Modifier)` function and `QuotesDestination.ROUTE`; the impl holds the concrete `QuotesEntryPoint` implementation, ViewModel, data sources, DTOs, mappers, and Compose screens. Alternative considered: putting routes/models in `api` and screens in `impl` — chosen, since the entry contract and route are the integration surface.
- **No navigation framework in the foundation.** The app hosts the quotes destination directly by binding the impl implementation to `QuotesEntryPoint` and calling `Content(...)`; `QuotesDestination.ROUTE` is the stable identifier later navigation wiring can build on. Alternative considered: adding Compose Navigation now — rejected as speculative for a single destination.
- **Compose + MVVM, no scheduler/stream ownership in composables.** The ViewModel owns state and orchestration; Compose is a pure renderer. This is mandatory for the assignment and is enforced as a spec requirement.
- **No DI framework decision here.** The DI approach is deferred to the change that first needs to inject a real dependency; this foundation may wire the empty screen directly. This avoids adding a library before it is used. (Open question below.)
- **OpenSpec artifacts are local-only, by explicit user choice.** The `/openspec/` tree stays git-ignored and is not treated as repository-wide documentation. Change artifacts live in this workspace only: they are intentionally not committed, pushed, or available in other clones. The Android gate and code review use the local artifacts as the source of truth; the trade-off is that the proposal is intentionally not portable to other clones, which is accepted rather than fixed by changing `.gitignore`.

## Risks / Trade-offs

- [The `api`/`impl` split can drift into duplication] → Keep `api` minimal (entry point + public UI models) and route all logic through `impl`; review when the quotes board lands.
- [Skipping generic `core` modules may cause some short-term duplication] → Accept duplication until a second feature consumes the code, then extract; this is the explicit assignment constraint.
- [Adding modules before any real behavior could churn the build] → Foundation is intentionally small and verified by a compiling, launchable skeleton.

## Open Questions

- Which dependency-injection approach (if any) to use when the quotes feature first needs injected dependencies. This does not change the specs, approach, or task breakdown of the foundation, and can be decided when implementing the quotes board.
