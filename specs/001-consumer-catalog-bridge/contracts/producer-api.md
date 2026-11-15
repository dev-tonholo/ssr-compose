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

## Writing a screen

Each held catalog has its own bridge, created by the compiler in the package the build setup names for it. A screen for one catalog version calls the bridge functions of that version, like any composable:

```kotlin
import com.example.app.bridge.v3.Greeting
import com.example.app.bridge.v3.PaymentSection
import com.example.app.bridge.v3.Style

@Composable
fun CheckoutScreen() {
    var paid by remember { mutableStateOf(false) }
    val consumer = LocalConsumer.current

    PaymentSection(
        title = if (paid) "Thank you" else "Pay now",
        onPayClick = { paid = true },
    ) {
        Greeting(title = "Order 42", style = Style.titleLarge)
    }
}
```

- State held with `remember` lives on the server, in that app's session, and is gone when the session ends.
- A lambda that throws changes nothing: what it wrote is thrown away, the screen stays as it was, and `onActionFailed` is called.
- `onPayClick = { ... }` is a `RemoteAction0`. It runs on the server when the user taps. A callback that carries a scan result is a `RemoteAction1<ByteArray>`. The pattern follows Kotlin's function types: `RemoteAction0`, `RemoteAction1<A>`, up to `RemoteAction4<A, B, C, D>`, one per number of arguments the app's callback has. There is no result type, because the app's callback returns at once and cannot wait for the server. Each is a function interface, so a lambda is enough.
- The trailing block fills the `content` slot with other bridge composables of the same version.
- `LocalConsumer.current` gives the session's `device`, `app` and `catalogVersion`. `device` is state: when the app reports a change, the screen recomposes and what it holds stays.
- A slot that passes arguments cannot be filled in this feature:
  - When the slot is optional, the bridge function has no parameter for it and the app uses its own default.
  - When the slot is required, the component cannot be placed. Its bridge function exists, and calling it is a compile error that names the component and the slot, and says that slots with arguments are not supported yet. `CameraScanner` and a `PaymentSection` whose title is a slot with arguments are in this group until remote composables exist.

## Screens for several catalog versions

Screens for two versions repeat their layout, because the two bridges are different functions. State and logic can be shared as plain Kotlin:

```kotlin
import com.example.app.bridge.v2.CounterCard as CounterCardV2
import com.example.app.bridge.v3.CounterCard as CounterCardV3

class HomeState { var count by mutableIntStateOf(0) }

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

Each one has no logic of its own. Its body is one call to a public function of this library, with its catalog version as a constant and a `Callback` for each callback parameter:

```kotlin
@Composable
fun PaymentSection(title: String, onPayClick: RemoteAction0, content: (@Composable () -> Unit)? = null) {
    ConsumerComponent(
        catalogVersion = "3",
        name = "PaymentSection",
        values = mapOf("title" to title),
        callbacks = mapOf("onPayClick" to Callback(arguments = emptyList(), convert = { { onPayClick() } })),
        slots = mapOf("content" to content),
    )
}
```

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
}
```

The default does nothing. The library depends on no logging library.

## What is public

`Producer` and its builder, `ProducerLimits`, `ProducerObserver`, `ConsumerComponent`, `Callback` and its value readers, `RemoteAction0` to `RemoteAction4`, `LocalConsumer` and its `Consumer` type. From `core` it also uses `ValueKind`, `CatalogVersion` and `ContractVersion`. Everything else is internal.
