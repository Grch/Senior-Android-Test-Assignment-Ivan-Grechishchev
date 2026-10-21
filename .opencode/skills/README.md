# Android / KMP Architecture Skills

A collection of eight opinionated skills for Android and Kotlin Multiplatform development. Claude Code loads them when installed globally; this repository keeps its OpenCode copies in `.opencode/skills/`.

## Prerequisites: Installing Claude Code

If you don't have Claude Code yet, install it first.

### macOS / Linux

Open a terminal and run:

```bash
curl -fsSL https://claude.ai/install.sh | bash
```

Then run `claude` to launch Claude Code and complete the authentication flow.

### Windows

Open **PowerShell** and run:

```powershell
irm https://claude.ai/install.ps1 | iex
```

Then run `claude` to launch Claude Code and complete the authentication flow.

> [Git for Windows](https://git-scm.com/downloads/win) is required. See the [official docs](https://docs.claude.com/en/docs/claude-code/quickstart) for detailed setup instructions.

## Installing the Skills

Each skill is a single markdown file (`SKILL.md`) inside its own folder. To install them, copy the eight skill folders into your Claude Code skills directory:

```
~/.claude/skills/
├── android-compose-ui/
│   └── SKILL.md
├── android-data-layer/
│   └── SKILL.md
├── android-di-hilt/
│   └── SKILL.md
├── android-error-handling/
│   └── SKILL.md
├── android-module-structure/
│   └── SKILL.md
├── android-navigation/
│   └── SKILL.md
├── android-presentation-mvi/
│   └── SKILL.md
└── android-testing/
    └── SKILL.md
```

Simply paste the eight folders into `~/.claude/skills/` and they'll be picked up automatically the next time you start a Claude Code session. OpenCode discovers the repository-local copies in `.opencode/skills/`; restart OpenCode after changing skills or config.

## What Each Skill Does

### android-module-structure

Defines the project-level blueprint: feature-layered modularization, Gradle convention plugins, version catalogs, and dependency rules. Claude will follow this structure whenever you ask it to create a new module, set up a project, or decide where code should live.

### android-compose-ui

Compose UI patterns including state ownership, side effects, recomposition, lazy layouts, animations, accessibility, and previews.

### android-error-handling

Typed `Result` wrappers, shared and feature-specific errors, and consistent error mapping across app layers.

### android-data-layer

Covers everything below the domain boundary: repository implementations, DTOs, Room entities and DAOs, Ktor `HttpClient` setup, safe-call helpers, token storage, offline-first patterns, and the shared `Result` / `DataError` types.

### android-presentation-mvi

The MVI presentation layer pattern: a single `State` data class, a sealed `Action` interface, one-time `Event` side-effects via `Channel`, ViewModel wiring, the Root/Screen composable split, `UiText` error mapping, and process-death handling with `SavedStateHandle`.

### android-navigation

AndroidX Navigation3 using `@Serializable` `NavKey` routes, an observable back stack, `NavDisplay` and `entryProvider`, `EntryProviderScope` feature entries, entry decorators, adaptive Scenes, and assembly in the `:app` module. This is Navigation3, not Navigation Compose / Navigation 2.

### android-di-hilt

Hilt dependency injection conventions: `@HiltAndroidApp` and `@AndroidEntryPoint`, one `@Module` per feature layer, `@Binds` / `@Provides` bindings installed in the appropriate component, constructor injection, and `@HiltViewModel` ViewModels injected in composables with `hiltViewModel()`.

### android-testing

Testing patterns and stack: JUnit 5 + AssertK + Turbine for ViewModel unit tests, `UnconfinedTestDispatcher`, fake repositories, `SavedStateHandle` testing, and Compose UI tests with `ComposeTestRule`.

## How Skills Work

When you give Claude Code a task — for example, *"create a new notes feature with a list and detail screen"* — it checks whether any installed skills match the request. If they do, Claude reads the corresponding `SKILL.md` files and follows the patterns described in them. Multiple skills can activate at once, so a feature-scaffolding request might pull in module-structure, data-layer, presentation-mvi, navigation, DI, and testing all together.

You don't need to reference skills by name. Just describe what you want to build and Claude will apply the right patterns automatically.
