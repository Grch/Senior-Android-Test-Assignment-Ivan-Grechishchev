## Why

The quotes implementation needs an explicit, framework-free integration/API boundary instead of living entirely in one Android feature module. Restoring separate API and implementation modules allows the app to consume the feature directly while preserving domain contracts and wires the Android-only implementation through Hilt.

## What Changes

- **BREAKING:** Replace `:feature:quotes` with real Gradle modules `:feature:quotes:api` and `:feature:quotes:impl`; keep `feature/quotes/` as a source-free parent directory.
- Move pure Kotlin quote models, repository/data-source contracts, top-securities result/error contracts, and fixed tickers into `api`; put Compose/MVVM, Retrofit, Moshi, OkHttp, DTOs, raw WebSocket source, repository implementations, and mappings in Android `impl`.
- Make `:app` depend directly on `impl` and render `QuotesRoot()` in `MainActivity`; remove the previous API entry-point/route/adapter contract and introduce no navigation framework.
- Configure Hilt and KSP in `app` and `impl`, use Hilt for data bindings and the `QuotesViewModel`, and leave API framework-free.
- Preserve the existing Tradernet WebSocket/REST behavior, lifecycle gating, fallback, ordering, display, and 300 ms update flash. Add or modify no tests; leave existing app tests untouched.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `android-module-foundation`: restore the API/implementation module topology and require Hilt-based app/implementation wiring while preserving the direct quotes root and current feature behavior.

## Impact

- `settings.gradle.kts`, app and quotes Gradle configuration, version catalog, manifest/Application/Activity, and quotes implementation source paths/packages.
- Maintained `android-module-foundation` behavior and active future `security-logos`/`min-step-price-formatting` module references. Archived OpenSpec records and the superseded `consolidate-quotes-module` change are unchanged.
- Hilt/KSP build-time code generation; `api` remains Kotlin/JVM with only its domain types and required coroutines-core dependency.
