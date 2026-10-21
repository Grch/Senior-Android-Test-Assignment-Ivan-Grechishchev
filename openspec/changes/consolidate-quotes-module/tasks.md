## 1. Active OpenSpec references

- [x] 1.1 Update only active future references from `:feature:quotes:impl` to `:feature:quotes` in the `security-logos` proposal/tasks and `min-step-price-formatting` proposal; verify active OpenSpec artifacts no longer refer to the removed module and archived records remain unchanged (targeted searches in both active change directories returned no `:feature:quotes:impl` or `:feature:quotes:api` references; archive files were not edited).

## 2. Consolidate the feature module

- [x] 2.1 Replace the `:feature:quotes:api` and `:feature:quotes:impl` includes with `:feature:quotes` and create its Android library configuration; verify the only leaf Gradle modules are `:app` and `:feature:quotes` (`./gradlew projects` succeeded and listed those modules, plus only the `:feature` grouping node).
- [x] 2.2 Move the complete feature implementation into `feature/quotes`, rename packages/imports from `.impl` to `.quotes`, and retain its Compose, ViewModel, Retrofit, OkHttp, data, and domain code; verified the 14 retained Kotlin sources now reside under `feature/quotes/src/main`, no moved source retains `.impl` imports, and the module build declares Compose/lifecycle plus Retrofit and OkHttp.
- [x] 2.3 Remove the API/impl build scripts, `QuotesEntryPoint`, `QuotesDestination.ROUTE`, and `DefaultQuotesEntryPoint`; confirmed targeted searches in app and consolidated feature sources return no removed contract, adapter, or module references, and `./gradlew projects` reports only the new quotes module.

## 3. Simplify app rendering

- [x] 3.1 Make `:app` depend only on `:feature:quotes` and simplify `MainActivity` to render `QuotesRoot()` inside the existing theme; verified the app dependency, direct `QuotesRoot()` call, and moved existing `Scaffold` inset behavior in the feature root.
- [x] 3.2 Confirm existing `app/src/test` and `app/src/androidTest` files are untouched and no test sources or coverage are added; `git diff --name-only -- app/src/test app/src/androidTest` and scoped `git status` both returned no changes.

## 4. Verification

- [x] 4.1 Verify `:app` and `:feature:quotes` lint/assemble passed; existing `:app:testDebugUnitTest` ran and passed; `:feature:quotes:testDebugUnitTest` was `NO-SOURCE`; no test sources were added or modified.
- [x] 4.2 Run `openspec validate consolidate-quotes-module` and confirm the new change artifacts validate; verified the modified `android-module-foundation` delta preserves the original scenario names and the active future references target `:feature:quotes` (`openspec validate consolidate-quotes-module` reported valid).

## 5. Domain and data boundaries

- [x] 5.1 Confirm Maven Central's latest stable Square Moshi release and compatible Kotlin adapter; add version-catalog aliases/dependencies and DTO adapters for inbound quote `q` frames and top-securities responses, removing manual `org.json` inbound parsing. Verified Maven metadata reports `moshi` and `moshi-kotlin` latest/release `1.15.2`, the `moshi-kotlin:1.15.2` POM depends on `moshi:1.15.2`, and both adapters compile; remaining `org.json` use is only for outbound request/subscription serialization.
- [x] 5.2 Implement `WebSocketRemoteDataSource` as a raw-frame `Flow<String>` transport boundary that owns open/close/cancel, the existing subscription send, 30-second ping, and 2-second reconnect/retry while preserving cancellation semantics. Verified it emits text unchanged, sends the unchanged one-frame subscription, cancels the underlying socket from `awaitClose`, and retries after 2 seconds; `TradernetClientFactory` preserves the shared OkHttp client's 30-second ping, and the quotes module compiles.
- [x] 5.3 Move the top-securities data-source abstraction to domain; add the domain `QuoteRepository` contract and data `TradernetQuoteRepository` that decodes raw quote frames through Moshi, maps DTOs to domain `Quote`, and ignores unrelated/malformed frames. Verified both contracts/models are under `domain`, Retrofit/Moshi/socket implementations and DTOs are under `data`, and targeted searches show no old quote socket-data-source contract remains.
- [x] 5.4 Refactor `QuotesViewModel` to use only domain contracts and retain foreground/screen-visible collection gating, active ticker order, UI state, partial merge, and 300 ms flash; keep implementation wiring at the feature root without DI/navigation changes. Verified the ViewModel imports only domain contracts for data dependencies, `QuotesRoot()` uses the manual feature factory, and the app still compiles.
- [x] 5.5 Run focused compile/lint/assemble checks where practical and `openspec validate`; add or modify no test sources and preserve the REST/WS wire contracts and API fallback behavior. `./gradlew :app:assembleDebug :feature:quotes:assembleDebug :app:lintDebug :feature:quotes:lintDebug` passed, `openspec validate consolidate-quotes-module` reported valid, `git diff --check` passed, and no test-source paths changed; no tests were added or run for this extension.
