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
- `@ConsumerComposable` or `@ProducerComposable` on a slot's type says what the slot accepts. With neither, it accepts either. Having both on the same slot type fails the build. What a slot accepts is recorded in the catalog and not checked in this feature.
- A parameter must be a kind the catalog can describe, or the build fails at that parameter. See [catalog-bundle.md](catalog-bundle.md).
- The marked composable and its parameters must have names that are plain Kotlin identifiers of at most 64 characters, or the build fails.
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
- The library collects density, orientation, screen size, theme, language, font scale and layout direction, and the space the app gives `SsrScreen`, and tells the producer when one changes.
- The screen is drawn as the producer measured and arranged it ([research.md R20](../research.md#r20-server-rendered-content-194)). The library places each part where the producer put it and arranges nothing itself. It does not show a screen until every app composable on it has been measured and placed by the producer; while a new layout is pending, it keeps showing the last one ([messages.md](messages.md#rules), rule 3b). It reports measurements at most 20 times per second, merging changes in between.
- `fonts` gives the app's font version and the font families it declared, by name ([font-bundle.md](font-bundle.md)). The version is sent in the introduction; when `fonts` changes to another version during a session, the library sends `FontVersionChanged` and keeps drawing each document with the fonts of the version it was laid out with ([messages.md](messages.md#rules), rule 3e). A server text in a family it does not have makes the screen `ScreenProblem.Mismatch`. Text with no family is drawn with the library's own Roboto. Its default holds no version and no family; the build creates it from the declared fonts (T055).
- The catalog version comes from the build. The developer supplies only `app`.
- The renderers for the marked composables are found by the library. Nothing has to be registered by hand.
- `connection` gives a new `Connection` for this screen alone. The library never names a transport. Two screens must not share one, because the producer refuses a second introduction.
- `unavailable` is optional. Without it, `ScreenProblemMessage` is shown with the library's text and look (see [The default problem message](#the-default-problem-message)). `retry` asks for the same screen again, or introduces the app again after `Refused`, `ConnectionEnded` or `AnswerUnreadable`.

## Doing it by hand

Everything the build creates on this side can be written by hand. A binding receives a `PlacedComponentScope`, the consumer's own type: it wraps core's `DocumentNode.Component` with the session, so `callback` can send a trigger and `slot` can draw the server-rendered content the producer laid out for that slot. An app with no marked composable passes `ComponentBindings {}`.

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

Only this by-hand form exists for now. The form without `bindings` and `catalogVersion` needs `CatalogVersionLoader` (T047) and the renderer index (T055).

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

- A parameter the screen did not give is absent. A parameter the screen set to nothing is `DataValue.None`. The strict read fails for both. The `OrNull` read returns null for both.
- Both reads fail for a value of another kind. There is no conversion: a whole number is not read as a decimal.
- `choice` and `choiceOrNull` take the enum as a type argument, and fail for an entry the enum does not have.
- Outside `SsrScreen`, every failure throws a `PlacedValueException`. Its message names the component, the parameter, the expected kind and the found kind. It has no component or parameter field.
- Inside `SsrScreen`, a failed read does not throw, because an exception during composition stops the app's recomposer. The read reports the component and returns a stand-in: an empty text or list, zero, false, or the first entry of the enum. An `OrNull` read returns null.
- Content with a reported problem is never drawn in the screen's window, so the user never sees a stand-in there. Content in a `Dialog` or `Popup` of a failing binding has its own window and can show the stand-in briefly, until the problem replaces the screen (2 draws measured in a `Dialog`, 1 in a `Popup`). #189 removes this limit. The whole screen is replaced by `unavailable` with `ScreenProblem.Mismatch` for the topmost component that failed. The next `Screen` message or a retry clears it.
- Effects in a binding that failed can run once. Code in the app's own binding that fails on a stand-in, such as `list.first()`, is the app's responsibility (FR-037).
- A `choice` read on an enum with no entries has no stand-in. The build will reject it (#188). Until then it throws as a last guard.
- Two scopes over equal placed components are equal, so a binding is skipped when its component did not change.

## Problems the user can be shown

```kotlin
sealed interface ScreenProblem {
    data class Refused(val kind: RefusalKind, val reason: String) : ScreenProblem
    data class Unavailable(val reason: String) : ScreenProblem
    data object ConnectionEnded : ScreenProblem
    data object AnswerUnreadable : ScreenProblem
    data class Mismatch(val component: String) : ScreenProblem
}
```

Approved by the owner on 2026-11-19 (#196, [research.md R19](../research.md#r19-text-the-user-reads-and-the-look-of-the-problem-message-196)). Before it, `ConnectionEnded` was an `Unavailable` and `AnswerUnreadable` a `Refused` with the kind `IntroductionNotValid`, each with an English reason written by the library.

- `Refused` and `Unavailable` carry the producer's `reason`. The library shows it as given and does not translate it. Its language is set by [messages.md](messages.md) rule 7. An app that wants its own words for a refusal maps `kind`. `Unavailable` has no kind, so an app can only replace its reason with one text of its own.
- `ConnectionEnded` means the connection failed or ended.
- `AnswerUnreadable` covers a message the app cannot read, a `Screen` or `ScreenUnavailable` that arrives before `Accepted`, and an `Accepted` with another contract version.
- `Mismatch` means the app received something it cannot draw: a value a binding cannot read, a component with no binding, or a text in a font family the app did not give `SsrScreen`. It should not happen. `component` names what could not be drawn: the component's name, or, for a font, the family's name. The library does not crash the app and shows no developer placeholder (FR-037). `component` is for the app's own reporting and never reaches the default message (FR-059).
- `retry` introduces the app again after `Refused`, `ConnectionEnded` and `AnswerUnreadable`, and asks for the same screen again after `Unavailable` and `Mismatch`.

An app that writes its own `unavailable` handles all five cases. For the two cases with no producer reason it can show its own text, or the library's:

```kotlin
@Composable
fun MyProblemScreen(problem: ScreenProblem, onRetry: () -> Unit) {
    val strings = LocalSsrComposeStringResourceManager.current
    val message = when (problem) {
        is ScreenProblem.Refused -> problem.reason
        is ScreenProblem.Unavailable -> problem.reason
        ScreenProblem.ConnectionEnded -> strings.stringResource(
            resource = SsrComposeStringResource.ScreenProblemConnectionEnded,
        )
        ScreenProblem.AnswerUnreadable -> strings.stringResource(
            resource = SsrComposeStringResource.ScreenProblemAnswerUnreadable,
        )
        is ScreenProblem.Mismatch -> strings.stringResource(resource = SsrComposeStringResource.ScreenProblemMismatch)
    }
    ...
}
```

## Text the user reads

Approved by the owner on 2026-11-19 (#196). Every text the library writes for the user has a key, and the app can replace each one. These types live in `dev.tonholo.ssrcompose.consumer.text`. The English manager is `internal/text/EnglishStringResourceManager.kt`.

```kotlin
enum class SsrComposeStringResource {
    ScreenProblemRetry,
    ScreenProblemMismatch,
    ScreenProblemConnectionEnded,
    ScreenProblemAnswerUnreadable,
}

interface SsrComposeStringResourceManager {
    @Composable
    fun stringResource(resource: SsrComposeStringResource, vararg arguments: Any): String
}

val LocalSsrComposeStringResourceManager: ProvidableCompositionLocal<SsrComposeStringResourceManager>
```

| Key | English text | Arguments |
|---|---|---|
| `ScreenProblemRetry` | Try again | none |
| `ScreenProblemMismatch` | This screen cannot be shown right now. | none |
| `ScreenProblemConnectionEnded` | The connection to the server ended. Please try again. | none |
| `ScreenProblemAnswerUnreadable` | The app could not read the answer from the server. | none |

- `LocalSsrComposeStringResourceManager` is a `staticCompositionLocalOf`. Its default is the library's English manager, which is internal. An app that provides nothing sees the English text above.
- A manager must return a text for every key and must not throw: a failure during composition stops the app's recomposer.
- Keep the manager stable between compositions, with `remember` as in the example below. The local is static, so a new manager recomposes everything below it, and `SsrScreen` can hold thousands of components.
- A key's arguments, when a key has any, are listed in the table and never carry internal details (FR-059).
- Adding a key is a source break for a manager with an exhaustive `when` and no `else`, so the app's build names the new text to translate.

An app that translates some texts and keeps the rest:

```kotlin
class AppStrings(private val fallback: SsrComposeStringResourceManager) : SsrComposeStringResourceManager {
    @Composable
    override fun stringResource(resource: SsrComposeStringResource, vararg arguments: Any): String =
        when (resource) {
            SsrComposeStringResource.ScreenProblemRetry -> stringResource(id = R.string.retry)
            else -> fallback.stringResource(resource = resource, arguments = arguments)
        }
}

val fallback = LocalSsrComposeStringResourceManager.current
val strings = remember(fallback) { AppStrings(fallback) }
CompositionLocalProvider(LocalSsrComposeStringResourceManager provides strings) {
    SsrScreen(...)
}
```

Plurals: no text needs one yet. When the first one does, the manager gains this lookup, with a default body that uses the English text, and a new enum `SsrComposePluralResource`. `quantity` picks the plural form and is not added to `arguments` by itself, as in Android's `pluralStringResource`.

```kotlin
@Composable
fun pluralResource(resource: SsrComposePluralResource, quantity: Int, vararg arguments: Any): String
```

## The default problem message

Approved by the owner on 2026-11-19 (#196). `SsrScreen`'s default `unavailable` is a public composable that an app can call with its own look. It follows the shape of Material components and their `Defaults` objects, with no Material dependency. These types live in `dev.tonholo.ssrcompose.consumer.problem`, one per file.

```kotlin
@Composable
fun ScreenProblemMessage(
    problem: ScreenProblem,
    retry: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ScreenProblemMessageColors = ScreenProblemMessageDefaults.colors(),
    textStyle: TextStyle = ScreenProblemMessageDefaults.MessageTextStyle,
    contentPadding: PaddingValues = ScreenProblemMessageDefaults.ContentPadding,
    spacing: Dp = ScreenProblemMessageDefaults.Spacing,
    maxContentWidth: Dp = ScreenProblemMessageDefaults.MaxContentWidth,
    retryContentPadding: PaddingValues = ScreenProblemMessageDefaults.RetryContentPadding,
    retryBorderWidth: Dp = ScreenProblemMessageDefaults.RetryBorderWidth,
)

@Immutable
class ScreenProblemMessageColors(
    val containerColor: Color,
    val contentColor: Color,
) {
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
    ): ScreenProblemMessageColors
}

object ScreenProblemMessageDefaults {
    @Composable
    fun colors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
    ): ScreenProblemMessageColors

    val MessageTextStyle: TextStyle = TextStyle(fontSize = 16.sp, textAlign = TextAlign.Center)
    val ContentPadding: PaddingValues = PaddingValues(all = 24.dp)
    val Spacing: Dp = 16.dp
    val MaxContentWidth: Dp = 480.dp
    val RetryContentPadding: PaddingValues = PaddingValues(all = 12.dp)
    val RetryBorderWidth: Dp = 1.dp
}
```

- The defaults are today's look. `colors()` gives black on white in light theme and white on black in dark theme, read from the system theme. A colour left as `Color.Unspecified` takes that default.
- The colours are grouped because they change together with the theme and must stay a readable pair, so their default is a `@Composable` function, as `ButtonColors` and `ButtonDefaults.buttonColors()` are in Material 3. The other values do not depend on the theme, so each is a plain value, as `ButtonDefaults.ContentPadding` is.
- `ScreenProblemMessageColors` is `@Immutable` and not a `data class`. Its `copy` keeps the old colour for a `Color.Unspecified` argument. It has `equals` and `hashCode`.
- `contentColor` colours the text and the retry button's border. It replaces the colour of `textStyle`. `retryBorderWidth` is a width, not a `BorderStroke`, so the border has no second colour.
- `spacing` is the gap between the message and the retry button. `maxContentWidth` caps the width of the message and the button, not of the space `modifier` gives.
- `ScreenProblemMessage` centres one message and one retry button in the space `modifier` gives it. It arranges nothing of the server's screen.
- The message shows the producer's `reason` for `Refused` and `Unavailable` as given. For the other cases and for the retry button it reads the matching key from `LocalSsrComposeStringResourceManager`.

```kotlin
SsrScreen(
    ...,
    unavailable = { problem, retry ->
        ScreenProblemMessage(
            problem = problem,
            retry = retry,
            modifier = Modifier.fillMaxSize(),
            colors = ScreenProblemMessageDefaults.colors(
                containerColor = AppColors.surface,
                contentColor = AppColors.onSurface,
            ),
            contentPadding = PaddingValues(all = 32.dp),
        )
    },
)
```

## What is public

In `consumer`: `SsrScreen`, `ComponentBindings`, `PlacedComponentScope`, `PlacedValueException` and `ScreenProblem`. Added by #196: `SsrComposeStringResource`, `SsrComposeStringResourceManager`, `LocalSsrComposeStringResourceManager`, `ScreenProblemMessage`, `ScreenProblemMessageColors` and `ScreenProblemMessageDefaults`. None of them needs an opt-in. Everything else in `consumer` is internal, including the English manager.

The `core` types this API uses are in the one list of public `core` types in [data-model.md](../data-model.md#what-is-public-in-core).
