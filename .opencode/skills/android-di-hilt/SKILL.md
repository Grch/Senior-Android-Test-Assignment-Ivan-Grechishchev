---
name: android-di-hilt
description: |
  Hilt dependency injection for Android - @HiltAndroidApp, @AndroidEntryPoint, @Module/@InstallIn bindings, constructor injection, @HiltViewModel, hiltViewModel() in Compose, qualifiers, and scoping. Use this skill whenever setting up Hilt, defining a Hilt module, providing or binding a repository, injecting a dependency, or wiring Hilt in the Application class. Trigger on phrases like "set up Hilt", "add a Hilt module", "inject a dependency", "DI module", "hiltViewModel", "@HiltViewModel", "@AndroidEntryPoint", "@Provides", "@Binds", "InstallIn", or "constructor injection".
---

# Android Dependency Injection (Hilt)

## Principles

- **Hilt is Android-only.** In Kotlin Multiplatform projects keep DI abstractions in common code and provide the Hilt-backed implementation in the Android source set.
- **Prefer constructor injection** (`@Inject constructor`) over field injection. Field injection (`@Inject lateinit var`) is only for Android framework types you do not own (Activity, Fragment, View, etc.).
- **One binding per concern.** Prefer an `@Inject` constructor; fall back to a `@Module` only when you cannot annotate the constructor (interfaces, third-party types, or instances that need configuration).
- **Install each module in the narrowest Hilt component** that can own the binding.
- The graph is generated at build time. `:app` applies the Hilt Gradle plugin and annotates the `Application`; modules are discovered through `@InstallIn` rather than being manually assembled.

---

## Setup

Apply the Hilt Gradle plugin to every module that uses Hilt and add the Hilt compile-time processor (KSP by default; use kapt only when KSP is not possible). Declare all Hilt artifacts through the version catalog rather than hardcoding versions. Then annotate the `Application` class:

```kotlin
@HiltAndroidApp
class App : Application()
```

`@HiltAndroidApp` is the only global entry point. Android components that inject dependencies need `@AndroidEntryPoint`:

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity()
```

---

## Constructor Injection

```kotlin
class RoomNoteDataSource @Inject constructor(
    private val dao: NoteDao
) : NoteLocalDataSource
```

Prefer this whenever the type is yours. A module is required only for interfaces, third-party types, and configured instances.

---

## Modules

```kotlin
@Module
@InstallIn(SingletonComponent::class)
interface NotesDataModule {
    @Binds
    @Singleton
    fun bindNoteRepository(impl: OfflineFirstNoteRepository): NoteRepository
}
```

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CoreDataModule {
    @Provides
    @Singleton
    fun provideHttpClient(engine: HttpClientEngine): HttpClient =
        HttpClientFactory.create(engine)
}
```

- `@Binds` — interface → implementation. Abstract function, no body, no allocation; use it in preference to `@Provides` for simple bindings.
- `@Provides` — factory methods, configured instances, and third-party types.
- `@InstallIn` — scope the module to a Hilt component: `SingletonComponent` for app-wide singletons, `ActivityRetainedComponent` for activity-retained state, `ViewModelComponent` for ViewModel-scoped bindings, and the activity/fragment/view components for shorter lifetimes.
- `@Singleton` (and the other scope annotations) should be applied deliberately. A binding without a scope is a new instance per injection.

---

## ViewModels

```kotlin
@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel()
```

Inject the ViewModel in a Root composable with `hiltViewModel()`:

```kotlin
@Composable
fun NoteListRoot(
    onNavigateToDetail: (String) -> Unit,
    viewModel: NoteListViewModel = hiltViewModel()
) { ... }
```

`hiltViewModel()` resolves from the current `ViewModelStoreOwner`, so the surrounding navigation host must provide one per destination. Never pass ViewModels down the composable tree.

---

## Qualifiers

Use a qualifier when two bindings share the same type:

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
```

Prefer the built-in `@ApplicationContext` / `@ActivityContext` qualifiers over injecting a raw `Context`.

---

## Naming Conventions

| Thing | Convention | Example |
|---|---|---|
| Hilt module | `<Feature><Layer>Module` | `NotesDataModule`, `NotesPresentationModule` |
| ViewModel | `@HiltViewModel` + `@Inject constructor` | `NoteListViewModel` |
| Qualifier | PascalCase annotation | `IoDispatcher`, `BaseUrl` |

---

## Checklist: Adding DI for a New Feature

- [ ] Annotate injectable classes with `@Inject constructor`, or add a binding for types you cannot annotate
- [ ] Define a `@Module @InstallIn(...)` in the layer that owns the dependency
- [ ] Use `@Binds` for interface → implementation and `@Provides` for constructed/third-party types
- [ ] Scope with `@Singleton` (or another component scope) only where a single instance is actually required
- [ ] Annotate ViewModels with `@HiltViewModel` and inject them in composables with `hiltViewModel()`
