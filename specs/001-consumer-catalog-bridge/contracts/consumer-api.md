# Contract: consumer library API

What an app developer writes. Sketches of the shape, not final signatures. The API is composable first and has no Android-only type in it.

## Marking composables

```kotlin
enum class Style { bodyMedium, titleLarge }

@ConsumerComposable
@Composable
fun Greeting(title: String, style: Style = Style.bodyMedium, modifier: Modifier = Modifier) { ... }

@ConsumerComposable
@Composable
fun PaymentSection(
    title: String,
    onPayClick: () -> Unit,
    content: @ConsumerComposable @Composable () -> Unit = {},
) { ... }
```

- `@ConsumerComposable` on a function puts it in the consumer catalog. Only a top-level composable function with no type parameters can be marked. A member, an extension or a generic one fails the build at that function.
- `@ConsumerComposable` or `@ProducerComposable` on a slot's type says what the slot accepts. With neither, it accepts either. This is recorded in the catalog and not checked in this feature.
- A parameter must be a kind the catalog can describe, or the build fails at that parameter. See [catalog-bundle.md](catalog-bundle.md).
- A modifier parameter is left out of the catalog and gets its default.

The annotations live in `core`, so common code can use them. The Gradle plugin adds `core` to the compile classpath of each compilation by itself, at the plugin's own version, so a module that applies `alias(libs.plugins.ssrcompose)` needs nothing else to mark composables.

## Showing a screen

```kotlin
SsrScreen(
    screen = "checkout",
    connection = { openConnection() },
    app = AppIdentity(version = "2.4.0", variant = "release", flavor = "play"),
    modifier = Modifier.fillMaxSize(),
    unavailable = { problem, retry -> MyProblemScreen(problem, onRetry = retry) },
)
```

- `SsrScreen` introduces the app, waits for the answer, opens the screen and shows it.
- The library collects density, orientation, screen size, theme and language, and tells the producer when one changes.
- The catalog version comes from the build. The developer supplies only `app`.
- The renderers for the marked composables are found by the library. Nothing has to be registered by hand.
- `connection` gives a `Connection`. The library never names a transport.
- `unavailable` is optional. Without it, a plain English message with a retry button is shown. `retry` asks for the same screen again, or introduces the app again after a refusal.

## Doing it by hand

Everything the build creates on this side can be written by hand:

```kotlin
val bindings = ComponentBindings {
    bind("Greeting") { placed ->
        Greeting(title = placed.text("title"), style = placed.choice("style") ?: Style.bodyMedium)
    }
    bind("PaymentSection") { placed ->
        PaymentSection(
            title = placed.text("title"),
            onPayClick = placed.callback("onPayClick"),
            content = placed.slot("content"),
        )
    }
}

SsrScreen(screen = "checkout", bindings = bindings, catalogVersion = CatalogVersion("3"), ...)
```

## Problems the user can be shown

```kotlin
sealed interface ScreenProblem {
    data class Refused(val kind: RefusalKind, val reason: String) : ScreenProblem
    data class Unavailable(val reason: String) : ScreenProblem
    data class Mismatch(val component: String) : ScreenProblem
}
```

`Mismatch` means the app received something it cannot draw. It should not happen. The app does not crash and shows no developer placeholder.

## What is public

`SsrScreen`, `ComponentBindings` and its `PlacedComponent` reader, `ScreenProblem`, and in `core`: `@ConsumerComposable`, `@ProducerComposable`, `AppIdentity`, `Connection`, `InMemoryConnection`. Everything else is internal.
