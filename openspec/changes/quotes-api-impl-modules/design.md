## Context

The working tree currently has the previous consolidation change's single Android `:feature:quotes` module, with pure Kotlin domain contracts and the Compose/MVVM and network implementation together. The maintained `android-module-foundation` contract likewise reflects the consolidated topology. See `proposal.md` for the requested architectural reversal; the delta spec defines the restored module and Hilt contract.

## Goals / Non-Goals

**Goals:**
- Restore a true Kotlin/JVM API module and an Android Compose implementation module, with implementation depending on API.
- Keep domain contracts and ticker/result models in API, and all Android/framework/network code in impl.
- Use Hilt/KSP-generated bindings for the app application root, feature ViewModel, network configuration, and API contract implementations.
- Preserve live quotes behavior, protocols, and visuals without adding tests or changing existing tests.

**Non-Goals:**
- Re-introducing the former `QuotesEntryPoint`/route adapter contract, Navigation Compose or any other navigation framework.
- Changing live Tradernet request/socket semantics, quote UI behavior, test coverage, or adding shared core/KMP modules.
- Editing the completed `consolidate-quotes-module` artifacts or archived OpenSpec history.

## Decisions

1. **Use two real leaf projects under a source-free parent.** Add `feature/quotes/api` and `feature/quotes/impl` Gradle roots and include `:feature:quotes:api` / `:feature:quotes:impl`. Keep the Kotlin/JVM API source package under `com.grch.tradernettestasignment.feature.quotes.api.domain`; place UI in `...feature.quotes.impl` and implementation data in its `.data` package. API uses `org.jetbrains.kotlin.jvm` and exposes `kotlinx-coroutines-core` via the catalog because `Flow` appears in its repository signature. Impl uses Android library + Compose and depends on API.
   - **Alternative considered:** keep the consolidated module or move only code into nested directories without Gradle roots. Rejected because the required topology is two actual Gradle modules.

2. **Keep direct app-to-root integration.** `:app` depends on impl directly and `MainActivity` calls `QuotesRoot()` inside the existing theme. Remove the old manual ViewModel factory and any previous entry point/route/binding code; do not add navigation.
   - **Alternative considered:** route app integration through a composable API adapter. Rejected because direct root rendering is requested and Hilt owns object construction.

3. **Use stable Hilt with AGP-9-compatible KSP.** The official Dagger Hilt Gradle setup currently documents Hilt `2.60.1` and KSP as the supported Kotlin processing path. The root build uses AGP `9.4.1` with built-in Kotlin; KSP `2.2.10-2.0.2` failed project configuration because it adds generated paths through the now-disallowed Kotlin source-set DSL. KSP's official `2.3.10` release notes include an AGP 9 built-in Kotlin fix; use stable `2.3.12`, which includes that fix. Apply the Hilt plugin and KSP in app and impl only; apply the Hilt Gradle plugin to both Android consumers so Hilt module aggregation works. `app` owns `@HiltAndroidApp` and `@AndroidEntryPoint`; impl owns its Hilt ViewModel, bindings, and providers. Use the standalone AndroidX Hilt lifecycle Compose artifact `1.4.0` for `hiltViewModel()` so no Navigation Compose APIs or dependency are introduced.
   - **Alternative considered:** kapt. Rejected because Dagger's official setup documents KSP for Kotlin and KSP `2.3.12` addresses the observed AGP 9 source-set incompatibility; no reason to switch processors is needed unless compile verification shows otherwise.

4. **Keep the existing network stack and payload contracts.** Provide one singleton OkHttp client with a 30-second ping, a Retrofit instance/service, and the existing Moshi Kotlin adapter configuration. Bind `QuoteRepository` to `TradernetQuoteRepository` and `TopSecuritiesDataSource` to the Retrofit implementation in a Hilt `SingletonComponent` module. Use `@Provides` for configured third-party instances/adapters and `@Binds` for interface-to-implementation bindings; inject the dependencies through constructors. Preserve raw frame flow/open-close/ping/retry policy in the WebSocket source and all top-securities parsing, mapping and fallback logic.
   - **Alternative considered:** build configured dependencies inside implementation constructors or retain a manual factory. Rejected because the change requires a Hilt-generated graph and scoped shared networking dependencies.

5. **Keep the API pure and minimal.** API contains only `Quote`, `QuoteRepository`, `TopSecuritiesDataSource`, top-securities result/error/domain contracts, and fixed ticker policy. No Hilt annotation, Android framework, Compose, or transport/serialization type crosses this boundary.
   - **Alternative considered:** expose a composable through API. Rejected because it makes the API Android/Compose-dependent and the app already integrates directly with impl.

6. **Pin only necessary catalog dependencies.** Add Hilt artifacts/plugin `2.60.1`, AGP-9-compatible KSP `2.3.12`, AndroidX Hilt lifecycle Compose `1.4.0`, and coroutines core `1.10.2` (a stable release supporting the current Kotlin toolchain). Keep existing Retrofit `3.0.0`, OkHttp `5.5.0`, and Moshi `1.15.2` versions unchanged.

7. **Preserve functionality and tests.** Relocate current source responsibilities rather than rewriting protocol or display behavior. Do not add or modify test files; existing app tests are left alone. Run focused Gradle project, compile/assemble, and lint tasks for API, impl, and app where practical.

## Risks / Trade-offs

- **[Risk]** Moving classes and package names can break API references or source-set wiring. → **Mitigation:** move contracts and implementation sources together, inspect the project graph and package imports, and compile each leaf module.
- **[Risk]** Hilt aggregation/KSP integration can fail with the repository's AGP/Kotlin versions. → **Mitigation:** use Hilt's official KSP workflow and the KSP release that includes AGP 9 built-in Kotlin support; prove generated Hilt code compiles with focused Gradle compile/assemble checks. Pause if the required versions remain incompatible rather than altering the required architecture silently.
- **[Risk]** Replacing manual construction with DI could accidentally change client sharing or socket lifecycle. → **Mitigation:** provide one singleton OkHttpClient and retain the current data-source and ViewModel lifecycle logic.
- **[Trade-off]** The app intentionally depends on the implementation module for its direct root composable. → This is the explicitly chosen app-to-feature integration; feature domain contracts remain available independently through API.

## Migration Plan

1. Create the new change artifacts against maintained `android-module-foundation`; then update only active future docs' module paths to impl.
2. Replace `:feature:quotes` with API and impl project roots; move pure contracts to Kotlin/JVM API and Compose/MVVM/network/data sources to Android impl.
3. Add catalog entries and generated Hilt/KSP setup to app and impl, including Application, manifest application name, Activity entry point, ViewModel injection, bindings, and configured providers.
4. Keep MainActivity's direct themed `QuotesRoot()` call and verify all live behavior remains intact.
5. Run project listing, API/impl/app compile or assemble, lint, OpenSpec validation, and diff whitespace checks; do not add or alter tests.

Rollback is a single source/module-graph revert; no persisted data, request format, or runtime protocol migration is involved.
