# TradernetTestAsignment — agent conventions

This project is also driven from OpenCode (`.opencode/`); this file is the Claude Code
counterpart. Keep both in sync when workflow rules change.

## Architecture conventions

- **Dependency injection: Hilt.** `@HiltAndroidApp` / `@AndroidEntryPoint`, `@Module`/`@InstallIn`,
  constructor injection, `@HiltViewModel` + `hiltViewModel()`. Do not use Koin.
- **Navigation: AndroidX Navigation3.** `NavDisplay`, `entryProvider`, `NavKey`, an observable back
  stack owned by the app. Do not use Navigation Compose / Navigation 2 (`NavHost`, `NavController`,
  `composable<T>`, `navigation<T>`, `rememberNavController`, `toRoute`) — treat their presence as a
  finding, not an equivalent API.
- Prefer the project skills (`android-module-structure`, `android-compose-ui`, `android-data-layer`,
  `android-di-hilt`, `android-error-handling`, `android-navigation`, `android-presentation-mvi`,
  `android-testing`) — load only the ones relevant to the task at hand.
- `android-di-hilt` is pinned project-locally under `.claude/skills/` because the globally installed
  skill on this machine is the Koin variant; this project-local copy takes precedence here.

## Subagents

| Agent | Model | Role |
|---|---|---|
| `coder` | Sonnet | Implements Android/Gradle/doc changes |
| `android-tester` | Sonnet | Runs the Gradle/lint/test gate, never edits code |
| `code-reviewer` | Opus | Correctness/architecture review, dispatched only after a green gate |
| `business-analyst` | Sonnet | Requirements, OpenSpec artifacts, docs — read-only |
| `explorer` | Haiku | Cheap read-only file/code search to scope a task before dispatching the above |

There is no separate "orchestrator" agent — the main session (you) plays that role directly via the
`Agent` tool, picking `subagent_type` from the table above.

## Routing a change

Classify a change by the files it actually touches, not by a vague "implementation task" label:

- **Route A — docs/spec/Markdown only** (`docs/**`, `*.md`, `README*`, `openspec/**`, `CLAUDE.md`,
  policy markdown under `.claude/`), no Android file touched: `business-analyst` (draft) → `coder`
  (apply) → `business-analyst` (re-review) until it explicitly returns **APPROVED**. Do not dispatch
  `android-tester` or `code-reviewer`, and do not run Gradle.
- **Route B — Android only** (app sources, Gradle build files, Android test sources), no docs
  touched: `coder` (implement + write tests) → `android-tester` (hard green gate) → `code-reviewer`
  (single pass, only once the gate is green).
- **Route C — mixed**: `business-analyst` drafts the doc/spec half, `coder` writes everything, then
  `android-tester` runs the Android portion of the gate first. While that gate is red, dispatch
  neither reviewer. Once green, dispatch `business-analyst` (doc half) and `code-reviewer` (Android
  half) in parallel; both must approve before anything is considered done.

**The gate before `code-reviewer` is hard, not a handoff:** dispatch it only after `android-tester`
reports every required `lint`, build/assemble, and local JVM unit test task green for every touched
module, plus any applicable `connectedDebugAndroidTest`. A task that failed, was skipped, or is
missing from the report keeps the gate red — send the findings back to `coder`, don't forward them
to `code-reviewer` as if the gate passed.

On a failure loop, keep it scoped to the route: a route A fix goes back to `coder` then
`business-analyst` only; a route B fix goes `coder` → focused `android-tester` → `code-reviewer`
once green again; a route C fix follows whichever half changed, and if `android-tester` fails on a
mixed change, re-run the focused Android checks before re-dispatching both pending reviews.

Use `explorer` (Haiku) first when you need to find where something lives before deciding how to
route or who to dispatch — it's cheap and read-only.

## Slash commands

`.claude/commands/opsx-*` mirror the OpenSpec workflow commands available in OpenCode
(`opsx-propose`, `opsx-apply`, `opsx-update`, `opsx-sync`, `opsx-archive`, `opsx-explore`). They
operate on the local `openspec/` tree (which is intentionally gitignored, like `.claude/`).
