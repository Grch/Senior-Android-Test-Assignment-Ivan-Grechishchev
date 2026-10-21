---
name: android-navigation
description: |
  AndroidX Navigation3 (androidx.navigation3) for Android/KMP - serializable NavKey routes, an observable back stack you own, NavDisplay + entryProvider, scoped NavEntry state, adaptive Scenes, and modular feature wiring. This is Navigation3, NOT Navigation Compose / Navigation 2 (which use NavHost, NavController, composable<T>, navigation<T>); those APIs must not be substituted for it. Use this skill whenever setting up Navigation3, defining Nav3 keys, adding a destination, wiring NavDisplay, managing the back stack, using entry decorators or Scenes, or migrating off Navigation 2. Trigger on phrases like "set up Navigation3", "add a Nav3 screen", "NavDisplay", "entryProvider", "NavKey", "rememberNavBackStack", "back stack", "scene strategy", or "Navigation 2 migration".
---

# Android / KMP Navigation (AndroidX Navigation3)

Navigation3 is a Compose-first navigation library. You own the back stack as an observable list, push and pop keys on it to navigate, and a `NavDisplay` renders the current entries. It is **not** Navigation Compose / Navigation 2: do not introduce `NavHost`, `NavController`, `composable<T>`, `navigation<T>`, `rememberNavController`, or `toRoute`. Those belong to the older API and must not be mixed in.

---

## Principles

- **The back stack is the navigation state.** You navigate by adding/removing keys (`backStack.add(...)`, `backStack.removeLastOrNull()`), not by calling a controller.
- **Routes are typed keys.** Every destination is a `@Serializable` type that implements `NavKey`.
- **One `NavDisplay` per window**, with an `entryProvider` that resolves each key to its content.
- **Each feature contributes its own entries** and does not depend on other features.
- Feature-to-feature navigation is expressed through callbacks or a shared navigator abstraction, never by importing another feature's implementation.

---

## Dependencies

Add the Navigation3 runtime and UI artifacts (and the lifecycle-ViewModel add-on if destinations use ViewModels) through the version catalog rather than hardcoding versions. Navigation3 requires a modern `compileSdk`; confirm the requirement for the version in use instead of pinning one here.

---

## Route Keys

Define each destination as a `@Serializable` `data object` (no arguments) or `data class` (with arguments) implementing `NavKey`:

```kotlin
// feature:notes:presentation
@Serializable data object NotesList : NavKey
@Serializable data class NoteDetail(val noteId: String) : NavKey
```

`@Serializable` + `NavKey` is what lets `rememberNavBackStack` persist the stack across configuration changes and process death. Avoid passing complex objects as keys — pass IDs and load data in the destination's ViewModel.

---

## Back Stack

For simple cases, create and remember the stack with `rememberNavBackStack`:

```kotlin
val backStack = rememberNavBackStack(NotesList)
```

For a plain, non-persistent stack, `remember { mutableStateListOf<Any>(NotesList) }` also works. Prefer `rememberNavBackStack` so state survives configuration changes and process death.

Navigate by mutating the stack:

```kotlin
backStack.add(NoteDetail(noteId = "abc123"))  // forward
backStack.removeLastOrNull()                   // back
```

Guard forward navigation that can be triggered more than once with `dropUnlessResumed` (or an equivalent lifecycle check).

---

## NavDisplay and entryProvider

`NavDisplay` renders the stack; `entryProvider` maps a key to its content via `entry<T>`:

```kotlin
NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider = entryProvider {
        entry<NotesList> {
            NotesListRoot(onNavigateToDetail = { backStack.add(NoteDetail(it)) })
        }
        entry<NoteDetail> { key ->
            NoteDetailRoot(noteId = key.noteId)
        }
    }
)
```

- `entry<T>` receives the concrete key, so arguments come from the key itself (no `toRoute()`).
- The `entryProvider` defines **content**, not parent/child hierarchy. There is no `navigation<T>` base route; a top-level key identifies its own back stack.
- Reusable feature sections can be `EntryProviderScope<T>` extension functions, mirroring the old `NavGraphBuilder` extensions:

```kotlin
fun EntryProviderScope<NavKey>.notesEntries(onOpenDetail: (String) -> Unit) {
    entry<NotesList> { NotesListRoot(onOpenDetail) }
    entry<NoteDetail> { key -> NoteDetailRoot(key.noteId) }
}
```

---

## Entry Decorators and ViewModels

`NavDisplay` supports `entryDecorators`. To scope `ViewModel`s to their `NavEntry` (a new instance per unique key), add the saveable-state and ViewModel-store decorators together:

```kotlin
NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()
    ),
    entryProvider = entryProvider { /* ... */ }
)
```

Destinations use an entry-scoped `LifecycleOwner` via `LocalLifecycleOwner.current`, so lifecycle-aware collection works directly inside the entry content.

For a ViewModel that needs the navigation key, use Hilt assisted injection and pass the key through `hiltViewModel`'s `creationCallback`. See the **android-di-hilt** skill for the Hilt setup.

---

## Adaptive Scenes

Scenes let one `NavDisplay` read more than one entry from the stack at once (list-detail, two-pane, dialog, bottom sheet). The core library ships a dialog scene strategy; list-detail and two-pane layouts are demonstrated as recipes — a `SceneStrategy` you adapt to the app — rather than a fixed built-in surface, so do not assume a specific helper name or dependency.

Attach metadata to entries and pass remembered scene strategies to `NavDisplay`:

```kotlin
NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    sceneStrategies = listOf(/* remembered SceneStrategy(s) */),
    entryProvider = entryProvider {
        entry<NotesList>(metadata = /* list-pane metadata */) { /* ... */ }
        entry<NoteDetail>(metadata = /* detail-pane metadata */) { key -> /* ... */ }
    }
)
```

Use dialog/bottom-sheet scene strategies for overlays and a two-pane strategy for large windows. Keep scene logic in navigation/UI code, not in ViewModels.

---

## Multiple Back Stacks

For bottom navigation with per-tab history, hold a `NavBackStack` per top-level route and show the stacks currently in use:

```kotlin
val backStacks = topLevelRoutes.associateWith { rememberNavBackStack(it) }
var topLevelRoute by rememberSaveable { mutableStateOf(startRoute) }
```

Switch tabs by changing `topLevelRoute`; push child destinations onto `backStacks[topLevelRoute]`. State for entries in inactive stacks is retained.

---

## Modular Feature Wiring

Feature modules expose their routes and an `EntryProviderScope` extension, and contribute entries to `:app` through Hilt multibinding (for example a `@IntoSet` set of installer lambdas). `:app` combines the installers into the single `entryProvider`. Features must not depend on each other.

---

## Naming Conventions

| Thing | Convention | Example |
|---|---|---|
| Nav key | `<Screen>` (`data object` / `data class`) | `NotesList`, `NoteDetail` |
| Feature entries | `EntryProviderScope<T>.<feature>Entries(...)` | `notesEntries(...)` |
| Navigator (optional) | `<Feature/Screen>Navigator` | `AppNavigator` |

---

## Checklist: Adding Navigation to a New Feature

- [ ] Define a `@Serializable` `NavKey` for each destination in `feature:presentation`
- [ ] Add an `EntryProviderScope<T>.<feature>Entries(...)` that maps keys to content
- [ ] Navigate by adding/removing keys on the back stack; never use a `NavController`
- [ ] Add `rememberViewModelStoreNavEntryDecorator()` (with the saveable-state decorator) when destinations use ViewModels
- [ ] Expose cross-feature navigation as callbacks or a shared navigator, never by importing another feature
- [ ] Wire feature entries into `:app`'s `NavDisplay`/`entryProvider` (Hilt multibinding is a good fit)
- [ ] Confirm no Navigation Compose / Navigation 2 symbols (`NavHost`, `NavController`, `composable<T>`, `navigation<T>`) remain
