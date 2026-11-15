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
- `limits` and `observer` are optional. The producer speaks every contract version this library has a codec for.

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

Each one has no logic of its own. Its body is one call to a public function of this library, with its catalog version as a constant and the argument kinds of each callback:

```kotlin
@Composable
fun PaymentSection(title: String, onPayClick: RemoteAction0, content: (@Composable () -> Unit)? = null) {
    ConsumerComponent(
        catalogVersion = "3",
        name = "PaymentSection",
        values = mapOf("title" to title),
        callbacks = mapOf("onPayClick" to Callback(action = onPayClick, arguments = emptyList())),
        slots = mapOf("content" to content),
    )
}
```

A callback with arguments lists their kinds with `ArgumentKind`, for example `Callback(action = onScan, arguments = listOf(ArgumentKind.Bytes))`. The producer checks each trigger's arguments against that list.

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

`Producer` and its builder, `ProducerLimits`, `ProducerObserver`, `ConsumerComponent`, `Callback`, `ArgumentKind`, `RemoteAction0` to `RemoteAction4`, `LocalConsumer` and its `Consumer` type. Everything else is internal.
