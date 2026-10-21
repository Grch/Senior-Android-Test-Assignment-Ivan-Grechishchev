## 1. Active OpenSpec references

- [x] 1.1 Update active `security-logos` and `min-step-price-formatting` proposal/task references from the removed consolidated `:feature:quotes` leaf to `:feature:quotes:impl` where implementation dependencies are described; confirmed searches in both active changes return no references to the obsolete leaf, and left the old `consolidate-quotes-module` and archive artifacts unchanged.

## 2. Restore API and implementation modules

- [x] 2.1 Replace `:feature:quotes` with `:feature:quotes:api` and `:feature:quotes:impl` in settings and add Kotlin/JVM API plus Android Compose implementation build files; verified `./gradlew projects` lists app and the API/impl leaves (with only Gradle grouping projects) and confirmed the parent has no build script or source files.
- [x] 2.2 Move quote/domain contracts, top-securities result/error contracts, and the ordered fixed ticker list into the pure Kotlin API module; add catalog coroutines-core exposure for `Flow` and verified `./gradlew :feature:quotes:api:compileKotlin` succeeds, with no Android/framework/network/serialization imports in API.
- [x] 2.3 Move Compose/MVVM and all Retrofit/Moshi/OkHttp DTO, parser, repository, and WebSocket implementation into Android impl; update packages/imports and declare its dependency on API; verified `./gradlew :feature:quotes:impl:compileDebugKotlin` succeeds.

## 3. Configure Hilt and app integration

- [x] 3.1 Add catalog versions/aliases and apply stable Hilt plus AGP-9-compatible KSP plugins and processor dependencies to app and impl only; verified app/impl plugin resolution and successful KSP processing/compilation; API does not apply Hilt.
- [x] 3.2 Add the Hilt `Application`, manifest application name, `@AndroidEntryPoint` Activity, and direct `QuotesRoot()` app dependency; remove manual ViewModel factory and any entry-point/route/adapter binding; verified app compiles and the root call remains inside the existing theme.
- [x] 3.3 Inject the Hilt ViewModel and bind API repository/data-source contracts to implementation classes; provide the shared 30-second-ping OkHttpClient, Retrofit client/service and Moshi/adapters in the narrowest applicable Hilt component; verified KSP generated and compiled the complete graph via successful impl/app Kotlin compile tasks.

## 4. Verify preservation and OpenSpec

- [x] 4.1 Inspect moved code and module boundaries to confirm live WebSocket open/close/raw frame/ping/retry behavior, lifecycle gating, screen-open API/fallback, ticker order/resubscription, quote merge/UI/colors/300 ms flash, and REST/WS wire contracts are preserved; verified the moved implementations retain those policies, searches found no entry-point/route/navigation APIs, and test-source path diff is empty.
- [x] 4.2 Ran `./gradlew :feature:quotes:api:compileKotlin :feature:quotes:impl:assembleDebug :app:assembleDebug :feature:quotes:impl:lintDebug :app:lintDebug` successfully, validated with `openspec validate quotes-api-impl-modules`, and confirmed `git diff --check` passes and no test paths changed. The final project listing also confirms the expected app/API/impl leaf topology; no test sources were added or modified, `:app:testDebugUnitTest` passed, and `:feature:quotes:api:test` plus `:feature:quotes:impl:testDebugUnitTest` were `NO-SOURCE`.
