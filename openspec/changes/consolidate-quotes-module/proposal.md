## Why

The quotes board is the application's only feature, and splitting it into separate API and implementation modules adds an entry contract and app-side wiring without a second consumer. Consolidating the feature keeps its existing behavior while making the module graph and app entry point direct.

## What Changes

- Replace `:feature:quotes:api` and `:feature:quotes:impl` with one Android library module, `:feature:quotes`, containing the full Compose, MVVM, networking, model, and mapping implementation. Keep presentation, domain contracts/models, and data implementations organized as packages within that module.
- Have `MainActivity` render `QuotesRoot()` directly inside the existing application theme, removing the entry-point interface, route constant, concrete adapter, and app-side binding.
- Parse inbound quote frames and top-securities responses with Square Moshi DTO adapters. Put raw-frame WebSocket lifecycle/reconnect in a data-layer remote source and map decoded frames to domain quotes in a domain-contract-backed repository.
- Keep `QuotesViewModel` dependent only on domain contracts; it owns visible/foreground collection gating, active tickers, UI state, quote merge, and flashes, while the data source owns socket reconnect/retry.
- Preserve the current live quote stream, API-on-screen-open default and complete fixed-list fallback, source ordering/subscription, 30-second ping, raw cancellation semantics, unauthenticated requests, REST GET `/api/` with `q`, quote values, row output, colors, and update flashes.
- Keep the project Android-only and add no tests or test coverage; leave existing tests untouched.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `android-module-foundation`: replace the API/implementation split and app-side entry binding with a single quotes module and direct app rendering while preserving the existing architectural constraints.

## Impact

- Gradle topology and module configuration in `settings.gradle.kts`, `app/build.gradle.kts`, and the consolidated `feature/quotes` module.
- Quotes implementation source paths/packages, data/domain responsibilities, Moshi dependencies, and the WebSocket/repository boundary; the REST and WebSocket wire contracts remain unchanged.
- `MainActivity` rendering and active, future OpenSpec proposal/task references to the quotes module.
- No changes to archived OpenSpec records, `.gitignore`, or existing app test sources.
