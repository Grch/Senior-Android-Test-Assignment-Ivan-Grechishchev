## Why

The project currently consists of a single `:app` module from the Android template, with no place to put the quotes feature. The assignment is Android-only and requires Jetpack Compose + MVVM, with several optional features (top-securities source, ticker logos, min-step formatting) that must be added without rewriting the app. A pragmatic multi-module foundation is needed now so every later change has a stable integration boundary.

## What Changes

- Add a two-module feature split: `:feature:quotes:api` (public integration boundary) and `:feature:quotes:impl` (feature implementation). `:app` remains the composition root and the only module that wires `api` to `impl`; as the composition-root exception it depends on both `api` and `impl`, while every other consumer integrates through `api` only.
- Introduce exactly three Gradle modules in total: `:app`, `:feature:quotes:api`, and `:feature:quotes:impl`. No other module is added by this change.
- Declare the new modules in `settings.gradle.kts` and give each a build script using the existing version catalog.
- Adopt MVVM + Jetpack Compose as the mandatory presentation architecture; the quotes feature ViewModel owns screen state and stream orchestration, while Compose only renders state and dispatches actions.
- Expose a minimal public entry contract from `:feature:quotes:api`: `QuotesEntryPoint` with a composable `Content(modifier: Modifier)` function and `QuotesDestination.ROUTE` as the stable destination identifier. `:feature:quotes:impl` provides the implementation, and the `:app` composition root binds it and presents the feature through the contract. No navigation framework is introduced.
- Explicitly avoid speculative generic `core` modules; shared code is extracted only when a second consumer appears.
- Android only. No iOS target, no KMP, and no DI framework. No test sources or test coverage are added by this change, and existing tests are not removed.

## Capabilities

### New Capabilities
- `android-module-foundation`: The Gradle module topology, dependency direction, composition-root wiring, and Compose/MVVM presentation contract for the Android app.

### Modified Capabilities
<!-- None: no existing specs exist in openspec/specs/. -->

## Impact

- `settings.gradle.kts`: add `:feature:quotes:api` and `:feature:quotes:impl` includes, yielding exactly `:app`, `:feature:quotes:api`, and `:feature:quotes:impl` with no `core:*` modules.
- New directories: `feature/quotes/api/`, `feature/quotes/impl/` with build scripts.
- `:app`: build script updated to depend on the quotes feature API and impl as the composition-root exception, and to host the composition root that binds `DefaultQuotesEntryPoint` to the `QuotesEntryPoint` contract; all other consumers depend on the API only.
- `gradle/libs.versions.toml`: add any Compose/AndroidX dependencies needed to compile the foundation.
- No application behavior is implemented by this change beyond a skeleton quotes entry point; the real quotes board is delivered by the `live-quotes-board` change, which depends on this foundation. No test sources or test coverage are added, and existing tests are not removed.
- Local-only OpenSpec storage (intentional): the root `/openspec/` tree is deliberately git-ignored and is not a repository-wide, portable deliverable. These change artifacts remain in this workspace only, are not committed or pushed, and are not available in other clones; they are local working records, not shared project documentation.
