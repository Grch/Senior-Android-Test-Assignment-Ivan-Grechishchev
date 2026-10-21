## 1. Module scaffolding

- [x] 1.1 Add `:feature:quotes:api` and `:feature:quotes:impl` to `settings.gradle.kts` and verify `:app`, `:feature:quotes:api`, and `:feature:quotes:impl` are the only declared modules with build scripts.
- [x] 1.2 Create `feature/quotes/api/build.gradle.kts` as an Android library with Compose enabled using the version catalog and `lintChecks(libs.slack.compose.lint.checks)` declared, and verify the module compiles.
- [x] 1.3 Create `feature/quotes/impl/build.gradle.kts` as an Android library with Compose enabled, `lintChecks(libs.slack.compose.lint.checks)` declared, and a dependency on `:feature:quotes:api`, and verify the dependency direction by inspecting the build script.
- [x] 1.4 Add any required Compose/AndroidX catalog entries to `gradle/libs.versions.toml` and verify the project configuration resolves without hardcoded versions.

## 2. Public API boundary

- [x] 2.1 Define `QuotesEntryPoint` with composable `Content(modifier: Modifier)` and `QuotesDestination.ROUTE` in `:feature:quotes:api`, and verify `:feature:quotes:impl` can reference both.
- [x] 2.2 Confirm `:feature:quotes:api` declares no dependency on `:feature:quotes:impl` or on `:app`, and verify by inspecting its build script.

## 3. Implementation module

- [x] 3.1 Create a minimal `:feature:quotes:impl` screen composable that renders placeholder state and forwards actions, and verify it compiles.
- [x] 3.2 Add a placeholder `ViewModel` in `:feature:quotes:impl` exposing an observable state flow, and verify the screen reads state from the ViewModel contract rather than owning socket/data logic.

## 4. App composition root

- [x] 4.1 Make `:app`, as the composition-root exception, depend on both `:feature:quotes:api` and `:feature:quotes:impl`, and verify the app module builds.
- [x] 4.2 Wire the `:app` composition root to bind the `:feature:quotes:impl` implementation to the `:feature:quotes:api` `QuotesEntryPoint` contract and present it via `Content(modifier)` with no navigation framework; verify `:app` installs and launches on an emulator/device — `:app:installDebug` succeeded on `Nexus_4_1280p_API_34` (`emulator-5554`), `adb am start` launched `.MainActivity`, the UI dump showed the Quotes placeholder and button, and tapping it showed "Action dispatched".

## 5. Verification and constraints

- [x] 5.1 Verify the only leaf modules with build scripts are exactly `:app`, `:feature:quotes:api`, and `:feature:quotes:impl` and that no `core:*`, iOS, or KMP targets were introduced by inspecting `settings.gradle.kts` and module build scripts.
- [x] 5.2 Verify no test modules or test sources were added by this change and that existing `:app` test sources are untouched.
- [x] 5.3 Run `./gradlew assembleDebug` and confirm a clean build of `:app`, `:feature:quotes:api`, and `:feature:quotes:impl`.
- [x] 5.4 Verify that only `:app` depends on `:feature:quotes:impl`, and that every other consumer depends on `:feature:quotes:api` only.
- [x] 5.5 Run `:feature:quotes:api:lintDebug` and `:feature:quotes:impl:lintDebug` and confirm the Slack Compose lint checks load and pass for both feature modules.

## Verification evidence (local, green)

Gates below were run locally on this workstation after the Android gate and code review. Outcomes are exact:

| Task | Verification outcome |
| --- | --- |
| 1.1 | `settings.gradle.kts` inspection confirms only `:app`, `:feature:quotes:api`, `:feature:quotes:impl`; all three assemble SUCCESS. |
| 1.2 | `:feature:quotes:api:assembleDebug` SUCCESS; `:feature:quotes:api:lint` SUCCESS. |
| 1.3 | `:feature:quotes:impl:assembleDebug` SUCCESS; `:feature:quotes:impl:lint` SUCCESS; build-script inspection confirms `impl` depends on `api`. |
| 1.4 | Version-catalog configuration resolves; `:app:assembleDebug`, `:feature:quotes:api:assembleDebug`, `:feature:quotes:impl:assembleDebug` all SUCCESS. |
| 2.1 | `:feature:quotes:impl:assembleDebug` SUCCESS (impl references `QuotesEntryPoint` and `QuotesDestination.ROUTE`). |
| 2.2 | `:feature:quotes:api` build-script inspection confirms no dependency on `impl`/`:app`; `:feature:quotes:api:assembleDebug` SUCCESS; `:feature:quotes:api:testDebugUnitTest` NO-SOURCE. |
| 3.1 | `:feature:quotes:impl:assembleDebug` SUCCESS and `:feature:quotes:impl:lint` SUCCESS. |
| 3.2 | `:feature:quotes:impl:assembleDebug` SUCCESS; inspection confirms screen reads state from the ViewModel contract. |
| 4.1 | `:app:assembleDebug` SUCCESS with `:app` depending on both `api` and `impl`. |
| 4.2 | Install/launch evidence is recorded inline in task 4.2 above and is not duplicated here. |
| 5.1 | Module list inspection shows exactly three leaf modules and no `core:*`, iOS, or KMP targets; all three assemble SUCCESS. |
| 5.2 | `:feature:quotes:api:testDebugUnitTest` and `:feature:quotes:impl:testDebugUnitTest` both NO-SOURCE (no test sources added); `:app:testDebugUnitTest` SUCCESS for pre-existing tests; no `src/androidTest` path changed, so connected tests N/A. |
| 5.3 | `:app:assembleDebug`, `:feature:quotes:api:assembleDebug`, and `:feature:quotes:impl:assembleDebug` all SUCCESS. |
| 5.4 | Inspection confirms only `:app` depends on `:feature:quotes:impl`; `:app:assembleDebug` SUCCESS. |
| 5.5 | `:feature:quotes:api:lint` and `:feature:quotes:impl:lint` SUCCESS with Slack Compose lint reporting no findings in either feature module; `:app:lint` SUCCESS with 11 warnings and 0 errors. |

Summary of the green gate: `:app:lint`, `:app:assembleDebug`, and `:app:testDebugUnitTest` SUCCESS; API and impl lint/assemble SUCCESS; API/impl `testDebugUnitTest` NO-SOURCE; no `src/androidTest` path changed so connected tests N/A.

These evidence notes are local-only: `/openspec/` remains intentionally git-ignored and is not committed, pushed, or available in other clones.
