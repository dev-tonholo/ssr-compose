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

Everything the build creates on this side can be written by hand. A binding receives a `PlacedComponentScope`, the consumer's own type: it wraps core's `PlacedComponent` with the session, so `callback` can send a trigger and `slot` can render content.

```kotlin
import dev.tonholo.ssrcompose.consumer.choiceOrNull

val bindings = ComponentBindings {
    bind("Greeting") { placed ->
        Greeting(title = placed.text("title"), style = placed.choiceOrNull<Style>("style") ?: Style.bodyMedium)
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

## Reading values

A binding reads a parameter by name from its `PlacedComponentScope`. Each kind has a strict read, which is a member of the scope, and an `OrNull` twin, which is an extension function in the same package, so a binding imports it:

| Kind | Strict read | OrNull read |
|---|---|---|
| text | `text(name): String` | `textOrNull(name): String?` |
| whole number | `whole(name): Long` | `wholeOrNull(name): Long?` |
| decimal number | `decimal(name): Double` | `decimalOrNull(name): Double?` |
| flag | `flag(name): Boolean` | `flagOrNull(name): Boolean?` |
| choice | `choice<E>(name): E` | `choiceOrNull<E>(name): E?` |
| list | `list(name): List<DataValue>` | `listOrNull(name): List<DataValue>?` |

- A parameter the screen did not give is absent. A parameter the screen set to nothing is `DataValue.None`. The strict read throws for both. The `OrNull` read returns null for both.
- Both reads throw for a value of another kind. There is no conversion: a whole number is not read as a decimal.
- `choice` and `choiceOrNull` take the enum as a type argument, and throw for an entry the enum does not have.
- Every failure is a `PlacedValueException`. Its message names the component, the parameter, the expected kind and the found kind. It has no component or parameter field.
- A `PlacedValueException` is thrown while the binding composes, and a `try/catch` cannot wrap a composable call, so it leaves the whole composition. How it becomes a `ScreenProblem.Mismatch`, which needs the component, is not decided. T054 must find a way to catch a failure thrown during a binding's composition and decide what happens to the rest of the screen.
- Two scopes over equal placed components are equal, so a binding is skipped when its component did not change.

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

In `consumer`: `SsrScreen`, `ComponentBindings`, `PlacedComponentScope`, `PlacedValueException` and `ScreenProblem`. Everything else in `consumer` is internal.

The `core` types this API uses are in the one list of public `core` types in [data-model.md](../data-model.md#what-is-public-in-core).
