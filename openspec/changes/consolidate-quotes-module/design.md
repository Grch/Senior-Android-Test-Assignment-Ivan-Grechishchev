## Context

The current project has `:app`, `:feature:quotes:api`, and `:feature:quotes:impl`. The implementation module owns the Compose screen, ViewModel, OkHttp WebSocket, Retrofit top-securities request, parsers, and quote models; the API module contains only an entry-point interface and route constant. `MainActivity` instantiates an adapter, binds it to that interface, and renders it through a `Scaffold`. This change consolidates those modules and retains layer boundaries as packages inside the one feature module. See `proposal.md` for motivation and `specs/android-module-foundation/spec.md` for the revised contract.

## Goals / Non-Goals

**Goals:**
- Keep the whole quotes feature in one Android library module and preserve its public root composable.
- Preserve all live stream, screen-open API selection/fallback, ordering, lifecycle, and rendering behavior without adding a new integration framework.
- Make the app entry point render the feature directly while retaining the existing application theme.
- Keep `domain` pure Kotlin with quote/top-securities models and repository/data-source abstractions; keep Retrofit, OkHttp, Moshi, DTOs, mappers, and data-source implementations in `data`.
- Keep `QuotesViewModel` dependent only on the domain `QuoteRepository` and top-securities data-source contract. The ViewModel retains foreground/screen-visible collection gating, ticker selection, UI state, merge, and flash ownership.
- Let the raw-frame WebSocket remote data source own opening/closing/cancellation, the 30-second OkHttp ping configuration, existing subscription send, and 2-second reconnect delay. It emits raw strings; a data repository decodes the verified `q` envelope and maps partial quote DTOs to domain quotes.
- Use Square Moshi 1.15.2 (`moshi` and compatible `moshi-kotlin`) for inbound quote and top-securities payload parsing, replacing the inbound `org.json` parsers.

**Non-Goals:**
- Changing the quote-source rules, REST/WebSocket protocols, displayed data, or visuals.
- Introducing DI, navigation, new feature/core modules, or test coverage.
- Editing existing test sources or archived OpenSpec records.

## Decisions

1. **Consolidate the old implementation module as `:feature:quotes`.** Move its Android library build configuration and all feature sources into `feature/quotes`; remove the old `api` and `impl` build scripts and their entry-point-only Kotlin sources. Use the `com.grch.tradernettestasignment.feature.quotes` package (with existing `data` and `domain` subpackages) so the old implementation suffix does not remain as a conceptual boundary. Keep Compose, MVVM/ViewModel, networking, models, and parsing together.
   - **Alternative considered:** retain `impl` packages after moving files. Rejected because it would preserve the removed split in source organization.

2. **Expose `QuotesRoot()` and wire domain dependencies at the feature composition boundary.** Keep the ViewModel constructor typed only to domain contracts. The Compose root's small manual ViewModel factory constructs the data implementations; no Hilt, Koin, service locator, or app-side factory is added. The ViewModel itself imports no data, OkHttp, Retrofit, or Moshi types.
    - **Alternative considered:** introduce a DI framework or a new API/route to manage the feature. Rejected because the feature has one consumer and no navigation requirement.

3. **Make `MainActivity` the direct host.** Within `TradernetTestAsignmentTheme`, call `QuotesRoot()` directly and remove `AppRoot`, `remember`, and the removed API/impl imports. Move the existing `Scaffold` inset handling into the feature root so `enableEdgeToEdge()` retains the current content insets and the same board layout without app-side feature integration scaffolding.
   - **Alternative considered:** remove the `Scaffold` inset handling entirely or keep it in `MainActivity`. Rejected because removing it could change the screen's edge-to-edge output, while leaving it in the app would retain unnecessary app-side presentation wrapping.

4. **Separate raw transport, domain contracts, and payload mapping within the single module.** `WebSocketRemoteDataSource` is an OkHttp-only transport boundary that emits raw frames and owns connection lifecycle and retries. `QuoteRepository` lives in domain; `TradernetQuoteRepository` collects raw frames, decodes the verified `q` envelope with a Moshi DTO adapter, maps optional payload fields into domain `Quote`, and ignores unrelated/malformed frames. `TopSecuritiesDataSource` also lives in domain, with its Retrofit implementation in data. No transport or JSON types cross into domain or ViewModel.
    - **Alternative considered:** keep quote parsing and reconnect in the ViewModel or transport source. Rejected because it couples presentation to the wire format or makes the transport responsible for domain mapping.

5. **Use Moshi for inbound DTO parsing and preserve the wire contracts.** Pin both `com.squareup.moshi:moshi` and `com.squareup.moshi:moshi-kotlin` to 1.15.2, confirmed as the latest release in Maven Central metadata. A shared Moshi instance with `KotlinJsonAdapterFactory` adapts DTOs for the quote array envelope/payload and top-securities JSON. Outbound REST `q` and WebSocket subscription formats remain unchanged; keep Retrofit's `ResponseBody` handling and unauthenticated requests.

6. **Move reconnect policy to the WebSocket data source.** Each collection opens one socket, sends the same single subscription frame on open, emits raw text frames, and closes by cancelling the OkHttp WebSocket when the flow is cancelled. Failures and normal disconnects retry after 2 seconds while collection remains active; cancellation stops retries immediately. Preserve the existing 30-second OkHttp ping interval.

7. **Preserve existing Android/runtime behavior and leave tests untouched.** Keep Retrofit, OkHttp, Compose and lifecycle dependencies, add Moshi to the consolidated feature module, and add no tests or test sources. Existing app test sources remain untouched.

5. **Update only active future documentation references.** Change `:feature:quotes:impl` references in the active `security-logos` proposal/tasks and `min-step-price-formatting` proposal to `:feature:quotes`. Leave archived OpenSpec artifacts, `.gitignore`, and archive records unchanged.

## Risks / Trade-offs

- **[Risk]** Moving package and source paths could break internal imports or ViewModel creation. → **Mitigation:** update all feature package/import references together and verify the consolidated module and app compile.
- **[Risk]** Moving the app `Scaffold` wrapper could affect insets or the board's visible area. → **Mitigation:** keep the same `Scaffold` content-inset handling in `QuotesRoot()`, retain `enableEdgeToEdge()` and the existing theme, and compile-check the final activity/screen composition without changing row UI.
- **[Trade-off]** Clean architecture boundaries are packages rather than Gradle modules. → This keeps the requested single feature module while maintaining pure domain contracts and data-layer implementations.

## Migration Plan

1. Create `:feature:quotes` by moving the implementation module build script and every implementation source, updating packages/imports and namespace.
2. Remove `:feature:quotes:api` and `:feature:quotes:impl` includes, scripts, API contract/route, and concrete adapter; make `:app` depend on `:feature:quotes` only.
3. Simplify `MainActivity` to call `QuotesRoot()` inside the existing theme, while hosting the existing scaffold/inset behavior inside the feature root.
4. Add Moshi 1.15.2 and inbound DTO adapters; move top-securities and quote repository contracts into domain; implement raw-frame WebSocket lifecycle/reconnect and quote decoding/mapping in data; wire domain dependencies at the root without changing DI/navigation.
5. Preserve API-on-open/fallback, ordering, partial quote updates, visibility/foreground gating, ping, cancellation, REST and socket wire formats, and the 300 ms flash. Update active future OpenSpec references and validate the change. Compile/assemble and lint where practical; do not add or modify test sources or test coverage. Existing required verification may still run (`:app:testDebugUnitTest` passes; `:feature:quotes:testDebugUnitTest` is `NO-SOURCE`).

Rollback consists of reverting this change as a unit; no persisted data, external API, or runtime protocol is migrated.
