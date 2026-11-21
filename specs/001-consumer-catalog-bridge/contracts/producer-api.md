# Contract: producer library API

What a server developer writes. Sketches of the shape, not final signatures. The library has no server framework and no transport in its API, and it holds no catalog at run time.

## Setting up

Screens are registered per catalog version:

```kotlin
val producer = Producer {
    catalog(version = "2") {
        screen(name = "home") { HomeScreenV2() }
    }
    catalog(version = "3") {
        screen(name = "home") { HomeScreenV3() }
        screen(name = "checkout") { CheckoutScreen() }
    }
    supportedContractVersions = setOf(ContractVersion(1))
    limits = ProducerLimits(callbacksPerSecond = 100)
    observer = PrintingObserver()
}

suspend fun onNewConnection(connection: Connection) = producer.serve(connection)
```

- The producer serves a catalog version while at least one screen is registered for it. An app of any other catalog version is refused at the introduction with the kind "catalog version not known or no longer supported".
- To stop serving a version, remove its `catalog` block. Its apps are then refused clearly.
- The app asks for a screen by name. The producer serves the screen registered under that name for the session's catalog version. A name with no screen for that version gets "screen unavailable", and the session stays accepted.
- `Producer { }` fails at once when a version is registered twice, or a screen name twice in one version.
- `serve` runs one session on one connection and returns when it ends.
- `supportedContractVersions` is the set of contract versions the producer accepts. By default it holds every version this library has a codec for. An app whose contract version is not in the set is refused: with the kind "contract version too old" when its version is below the highest version in the set, including a version in a gap of the set (the set is `{1, 3}` and the app speaks 2), and with "contract version too new" when it is above the highest. To stop a contract version, leave it out of the set. A set that is empty, or that holds a version this library has no codec for, makes `Producer { }` fail at once.
- `supportedContractVersions`, `limits` and `observer` are optional.
- The language of a `Refused` and `ScreenUnavailable` reason follows [messages.md](messages.md) rule 7. Which languages beyond English the producer has texts for is settled by T051.

## Writing a screen

A screen is plain Compose UI foundation code, run and laid out on the producer ([research.md R20](../research.md#r20-server-rendered-content-194)). Each held catalog has its own bridge, created by the compiler in the package the build setup names for it, and a screen places the app's components by calling the bridge functions of its catalog version, like any composable:

```kotlin
import com.example.app.bridge.v3.Greeting
import com.example.app.bridge.v3.PaymentSection
import com.example.app.bridge.v3.server.Style

@Composable
fun CheckoutScreen() {
    var paid by remember { mutableStateOf(false) }
    val brand = LocalConsumer.current.fonts.family(name = "brand")

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        BasicText(text = "Your order", style = TextStyle(fontFamily = brand, fontSize = 24.sp))
        PaymentSection(
            title = if (paid) "Thank you" else "Pay now",
            onPayClick = { paid = true },
        ) {
            Row(modifier = Modifier.background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)).padding(8.dp)) {
                BasicText(text = "Order 42")
                Greeting(title = "Ships today", style = Style.titleLarge)
            }
        }
        LocalSwitcher(variants = 2) { variant ->
            BasicText(text = if (variant == 0) "Show details" else "Hide details", modifier = Modifier.switchTo(1 - variant))
        }
    }
}
```

Server content:

- The producer wraps every screen in its own `Box` and measures it with the device's density, font scale, layout direction and the space the app gives the screen.
- Supported: `Box`, `Column`, `Row`, `Spacer`, `BasicText` with one style, `verticalScroll` and `horizontalScroll`, size, padding, weight and alignment modifiers, `background` and `border` with a color and a rectangle or rounded corner shape, `clickable`, and semantics (`contentDescription`, `heading`, `role`).
- Refused with `ScreenUnavailable` and `onScreenNotSendable`: images, brushes, other shapes, lazy lists, text with several styles, a font family the session's font bundle does not have, a weight or italic its font family has no file for, more than 10,000 app components, and an app component inside a switcher variant. Custom drawing (`Canvas`, `drawBehind`) is not seen by the producer and is not sent.
- `LocalSwitcher(variants, initial = 0, modifier) { variant -> }` composes and lays out every variant; `Modifier.switchTo(variant)` inside it shows that variant on the device at once, in a box of the largest variant's size, with no round trip. The device then sends `Switched`, the switcher's selected variant becomes that one, and the producer lays the screen out again so the content around it moves into place ([messages.md](messages.md#screen-content)). 2 to 16 variants.
- `LocalConsumer.current` gives the session's `device`, `app`, `catalogVersion` and `fonts`. `device` is state: when the app reports a change, the screen recomposes and what it holds stays.
- `LocalConsumer.current.fonts.family(name)` gives a font family of the session's font bundle ([font-bundle.md](font-bundle.md)). It fails with a clear message for a name that bundle does not have. The default family it holds is the library's own Roboto, which the device draws with too. A server text must name a family: a text with no family, or with `FontFamily.Default`, stops the screen with `onScreenNotSendable`, since Compose has no public way to remap the default ([ADR 0020](../../../docs/adr/0020-bundle-roboto-as-the-default-font.md)). T128 adds `fonts` to `Consumer`.
- The font bundles the producer holds come from the build (`ssrFontResources`), or by hand: `fonts(version = "1") { family(name = "brand") { font(bytes = interRegular, weight = 400) } }` in `Producer { }`. A session gets the bundle its app named in the introduction, and an app whose font version the producer does not hold is refused. A missing or unreadable font file makes `Producer { }` fail at start, as does a missing Skiko runtime for the server's OS.
- `ProducerLimits` also has `measuringRounds` (4 by default), `measurementsPerSecond` (30 `ComponentMeasured` and `ViewportChanged` messages per second by default, apart from the trigger limit) and `documentBytes` (8 MiB by default).

App components through the bridge:

- Choice enums live in a subpackage of the bridge package named after the server module, `<bridgePackage>.<moduleSegment>.<ChoiceName>` (above, `server`), and are `internal` to that module ([ADR 0010](../../../docs/adr/0010-per-module-choice-enums-in-bridge-subpackage.md), [research.md R18](../research.md#r18-server-bridge-kinds-choice-enums-and-consumercomponent-t048-design)). A screen's `main` and `test` compilations share the same import and enum types.
- Because the catalog collapses `Int` and `Long` into `whole`, and `Float` and `Double` into `decimal`, bridge parameters use `Long` for whole numbers and `Double` for decimal numbers. List parameters use `List<String>`, `List<Long>`, `List<Double>`, `List<Boolean>` or `List<<ChoiceEnum>>`.
- State held with `remember` lives on the server, in that app's session, and is gone when the session ends.
- A lambda that throws changes nothing: what it wrote is thrown away, the screen stays as it was, and `onActionFailed` is called.
- `onPayClick = { ... }` is a `RemoteAction0`. It runs on the server when the user taps. A callback that carries a scan result is a `RemoteAction1<ByteArray>`. The pattern follows Kotlin's function types: `RemoteAction0`, `RemoteAction1<A>`, up to `RemoteAction4<A, B, C, D>`, one per number of arguments the app's callback has. There is no result type, because the app's callback returns at once and cannot wait for the server. Each is a function interface, so a lambda is enough.
- The trailing block fills the `content` slot with server content written with foundation, which can hold other bridge composables of the same version. The producer lays it out in the space the app's component gives the slot.
- A slot that passes arguments cannot be filled in this feature:
  - When the slot is optional, the bridge function has no parameter for it and the app uses its own default.
  - When the slot is required, the component cannot be placed. Its bridge function exists, and calling it or referencing it is a compile error that names the component and the slot, and says that slots with arguments are not supported yet. The function carries `@UnplaceableBridge`, an annotation of the producer library that belongs to the compiler plugin: do not write it in code. The compiler plugin reads it from the created bridge. A created bridge function is internal to the module that creates it ([research.md R9](../research.md#r9-one-bridge-per-held-catalog)). A call that still gets past the compiler throws the same message when the function is composed. `CameraScanner` and a `PaymentSection` whose title is a slot with arguments are in this group until remote composables exist.

## Screens for several catalog versions

Screens for two versions repeat their layout, because the two bridges are different functions. State and logic can be shared as plain Kotlin:

```kotlin
import com.example.app.bridge.v2.CounterCard as CounterCardV2
import com.example.app.bridge.v3.CounterCard as CounterCardV3

class HomeState { var count by mutableLongStateOf(0L) }

@Composable
fun HomeScreenV2(state: HomeState = remember { HomeState() }) {
    CounterCardV2(count = state.count, onAddOne = { state.count++ })
}

@Composable
fun HomeScreenV3(state: HomeState = remember { HomeState() }) {
    CounterCardV3(count = state.count, onAddOne = { state.count++ })
}
```

This is a known cost of keeping older apps working without adapting screens for them. A composable that a newer catalog removed is still in the older versions' bridges, so screens for older apps can keep using it.

The guard: every bridge function passes its catalog version to `ConsumerComponent` as a constant. When a screen places a component from the bridge of a version other than the session's, for example a screen registered for "2" that calls a `bridge.v3` function, the producer does not send that screen. The app is told the screen cannot be shown, and `onComponentFromOtherCatalog` is called with the composable, the screen and both versions.

## What a bridge function is

Each one has no logic of its own. Its body is one call to `ConsumerComponent`, passing its catalog version as a `CatalogVersion` constant, `DataValue?` entries for its value parameters (with `null` meaning omitted and `DataValue.None` meaning an explicit nothing), a `Callback?` for each callback parameter (with `null` meaning omitted), and a slot lambda for each slot parameter (with `null` meaning omitted):

```kotlin
@InternalSsrComposeApi
@Composable
public fun ConsumerComponent(
    catalogVersion: CatalogVersion,
    name: String,
    values: Map<String, DataValue?> = emptyMap(),
    callbacks: Map<String, Callback?> = emptyMap(),
    slots: Map<String, (@Composable () -> Unit)?> = emptyMap(),
)
```

Before T065 introduces `Callback`, US1 uses a transitional `actions: Map<String, RemoteAction1<String>?> = emptyMap()` parameter in place of `callbacks` ([research.md R18](../research.md#2-interfaces-and-contracts)).

Example bridge function (compiler-generated bridges attach `@OptIn(InternalSsrComposeApi::class)` in FIR so server callers never need their own opt-in; a hand-written bridge writes `@OptIn(InternalSsrComposeApi::class)` itself):

```kotlin
@OptIn(InternalSsrComposeApi::class)
@Composable
internal fun PaymentSection(
    title: String,
    tags: List<String>? = null,
    onPayClick: RemoteAction0,
    content: (@Composable () -> Unit)? = null,
) {
    ConsumerComponent(
        catalogVersion = CatalogVersion("3"),
        name = "PaymentSection",
        values = mapOf(
            "title" to DataValue.Text(title),
            "tags" to tags?.let(::textListValue),
        ),
        callbacks = mapOf("onPayClick" to Callback(arguments = emptyList(), convert = { { onPayClick() } })),
        slots = mapOf("content" to content),
    )
}
```

For list parameters, `producer` provides five list-to-`DataValue.ListValue` helpers (`textListValue`, `wholeListValue`, `decimalListValue`, `flagListValue`, `choiceListValue`) marked with `@InternalSsrComposeApi` for generated and hand-written bridges. The full kind mapping table lives in [research.md R18](../research.md#parameter-kind-mapping).

### What a bridge gives the producer for one callback

A `Callback` holds two things:

- `arguments`: the kinds of the callback's arguments, as `ValueKind` from `core`, the same type the catalog uses. Empty when the callback has none.
- `convert`: a conversion from the values the app sent (`DataValue`s) to the lambda's typed arguments. It returns the call to run. This is where a choice entry becomes its enum, a whole number becomes `Int` or `Long`, a decimal becomes `Float` or `Double`, and a list becomes a typed list.

A sketch of the shape, not final signatures. The value readers (`entry`, `whole`, `list`) are helpers this library gives generated and hand-written bridges:

```kotlin
"onPick" to Callback(
    arguments = listOf(ValueKind.Choice(listName = "Style"), ValueKind.Whole),
    convert = { values ->
        val style = enumValueOf<Style>(values.entry(index = 0))
        val count = Math.toIntExact(values.whole(index = 1))
        { onPick(style, count) }
    },
)
```

When a trigger arrives, the producer:

1. Checks the number and the kinds of the values against `arguments`.
2. Runs `convert`. An unknown choice entry, a number that does not fit its type, or a list with one wrong element makes `convert` fail.
3. Runs the returned call, inside a snapshot, as research.md R10a says.

A failure in step 1 or step 2 is a refused trigger: nothing runs, the screen stays as it was, the session carries on, and `onTriggerRefused` is called. It is never `onActionFailed`, which is only for a call that fails while it runs in step 3.

Parameters the app can leave out:

- A parameter that is not required in the catalog becomes nullable in the bridge, with `null` as its default. `null` means "not given": nothing is sent for it, and the app's composable uses its own default. The server never knows that default.
- A required parameter whose type is nullable in the app stays required and nullable in the bridge. `null` is then sent as an explicit "nothing".
- One limit follows: for a parameter that is both nullable and optional, the server cannot send an explicit "nothing". Leaving it out and sending nothing look the same, and the app's default applies.

A developer can write the same thing by hand, so the library works and can be tested without the compiler plugin.

## Observer

```kotlin
interface ProducerObserver {
    fun onRefused(kind: RefusalKind, introduction: Introduction?)
    fun onComponentFromOtherCatalog(component: String, screen: String, session: CatalogVersion, bridge: CatalogVersion)
    fun onScreenNotRegistered(screen: String, catalogVersion: CatalogVersion)
    fun onTriggerIgnored(action: ActionId, screen: String)
    fun onTriggerRefused(action: ActionId, screen: String)
    fun onTriggerDropped(action: ActionId, screen: String)
    fun onActionFailed(action: ActionId, screen: String, cause: Throwable)
    fun onScreenStopped(screen: String, cause: Throwable)
    fun onScreenNotSendable(screen: String, reason: String)
    fun onLayoutNotSettled(screen: String)
}
```

The default does nothing. The library depends on no logging library.

## What is public

In `producer`:

- Server developer API: `Producer` and its builder, `ProducerLimits`, `ProducerObserver`, `RemoteAction0` to `RemoteAction4`, `LocalConsumer` and its `Consumer` type with `ConsumerFonts`, and `LocalSwitcher` with `LocalSwitcherScope`. The opt-in annotation `InternalSsrComposeApi` moves to `core` ([ADR 0014](../../../docs/adr/0014-one-opt-in-marker-for-internal-api.md)).
- Bridge entry points marked `@InternalSsrComposeApi` (`@RequiresOptIn(level = RequiresOptIn.Level.WARNING)`): `ConsumerComponent`, `textListValue`, `wholeListValue`, `decimalListValue`, `flagListValue`, `choiceListValue`, `Callback` and its value readers (added in T065), and `UnplaceableBridge`. The compiler plugin opts generated bridge functions in automatically, so server screens never need an opt-in; hand-written bridges opt in with `@OptIn(InternalSsrComposeApi::class)`. The compiler plugin owns `UnplaceableBridge` and puts it on a bridge that cannot be placed; it has to be public so the plugin can read it from compiled code, and developers must not write it.

Everything else in `producer` is internal.

The `core` types this API uses are in the one list of public `core` types in [data-model.md](../data-model.md#what-is-public-in-core).
