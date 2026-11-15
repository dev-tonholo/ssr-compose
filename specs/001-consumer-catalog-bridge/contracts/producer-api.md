# Contract: producer library API

What a server developer writes. Sketches of the shape, not final signatures. The library has no server framework and no transport in its API.

## Setting up

```kotlin
val producer = Producer(
    catalogs = CatalogSet.fromResources(bridge = "3", supported = setOf("2", "3")),
    supportedContractVersions = setOf(ContractVersion(1)),
    screens = mapOf("checkout" to { CheckoutScreen() }),
)

suspend fun onNewConnection(connection: Connection) = producer.serve(connection)
```

- `Producer(...)` checks the catalog set and fails at once on a bad one.
- `serve` runs one session on one connection and returns when it ends.
- `limits` and `observer` are optional.

## Writing a screen

The bridge functions are created by the compiler from the bridge catalog. A server developer calls them like any composable:

```kotlin
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

    OrElse(replacement = { Greeting(title = "Update the app to scan", style = Style.bodyMedium) }) {
        DocumentScanner(onScan = { bytes -> handle(bytes) })
    }
}
```

- State held with `remember` lives on the server, in that app's session, and is gone when the session ends.
- A lambda that throws changes nothing: what it wrote is thrown away, the screen stays as it was, and `onActionFailed` is called.
- `onPayClick = { ... }` is a `RemoteAction0`. It runs on the server when the user taps. `onScan` is a `RemoteAction1<ByteArray>`. The pattern follows Kotlin's function types: `RemoteAction0`, `RemoteAction1<A>`, up to `RemoteAction4<A, B, C, D>`, one per number of arguments the app's callback has. There is no result type, because the app's callback returns at once and cannot wait for the server. Each is a function interface, so a lambda is enough.
- The trailing block fills the `content` slot with other bridge composables.
- `OrElse` shows its replacement when a component inside it is missing for the connected app. Without it, a missing component stops the screen.
- `LocalConsumer.current` gives the session's `device`, `app`, `catalogVersion` and `has("DocumentScanner")`. `device` is state: when the app reports a change, the screen recomposes and what it holds stays.
- A slot that passes arguments cannot be filled in this feature:
  - When the slot is optional, the bridge function has no parameter for it and the app uses its own default.
  - When the slot is required, the component cannot be placed. Its bridge function exists, and calling it is a compile error that names the later feature. `CameraScanner` and a `PaymentSection` whose title is a slot with arguments are in this group until remote composables exist.

## What a bridge function is

Each one has no logic of its own. Its body is one call to a public function of this library:

```kotlin
@Composable
fun PaymentSection(title: String, onPayClick: RemoteAction0, content: (@Composable () -> Unit)? = null) {
    ConsumerComponent(
        name = "PaymentSection",
        values = mapOf("title" to title),
        callbacks = mapOf("onPayClick" to onPayClick),
        slots = mapOf("content" to content),
    )
}
```

Parameters the app can leave out:

- A parameter that is not required in the catalog becomes nullable in the bridge, with `null` as its default. `null` means "not given": nothing is sent for it, and the app's composable uses its own default. The server never knows that default.
- A required parameter whose type is nullable in the app stays required and nullable in the bridge. `null` is then sent as an explicit "nothing".
- One limit follows: for a parameter that is both nullable and optional, the server cannot send an explicit "nothing". Leaving it out and sending nothing look the same, and the app's default applies.

A developer can write the same thing by hand, so the library works and can be tested without the compiler plugin.

## Observer

```kotlin
interface ProducerObserver {
    fun onRefused(kind: RefusalKind, introduction: Introduction?)
    fun onComponentMissing(component: String, screen: String, catalogVersion: CatalogVersion)
    fun onParameterLeftOut(component: String, parameter: String, catalogVersion: CatalogVersion)
    fun onTriggerIgnored(action: ActionId, screen: String)
    fun onTriggerDropped(action: ActionId, screen: String)
    fun onActionFailed(action: ActionId, screen: String, cause: Throwable)
    fun onScreenStopped(screen: String, cause: Throwable)
}
```

The default does nothing. The library depends on no logging library.

## What is public

`Producer`, `CatalogSet`, `ProducerLimits`, `ProducerObserver`, `ConsumerComponent`, `RemoteAction0` to `RemoteAction4`, `OrElse`, `LocalConsumer` and its `Consumer` type. Everything else is internal.
