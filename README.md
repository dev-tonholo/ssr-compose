# ssr-compose

A proof of concept for server-side rendering with Jetpack Compose, where composables are shared in both directions between a server and an app.

The idea came out of a discussion about A2UI and AndroidX Remote Compose. The full discussion is in [docs/session-export.md](docs/session-export.md). Read it first.

## The idea

A server runs composable logic and sends the result to a thin client. One screen can mix two kinds of output:

- When the client already has a composable (from a shared Kotlin module), the server sends a reference to it with serializable parameters, and the client renders it natively. This is the A2UI half.
- When the client doesn't have it, the server sends that subtree as a recorded document of layout and draw operations, and the client plays it. This is the Remote Compose half.

## What the PoC should prove first

One shared composable, one server-only composable, and a server that emits a reference for the first and a recorded fragment for the second.

## Not verified yet

- Whether Compose logic can run on a plain JVM server. This decides whether the design is buildable.
- Whether AndroidX Remote Compose can create a document without an Android `Context`, and which composables it can capture.
- Whether its capture API emits a new document when content changes.
- How Cash App's Redwood solved the same problem, and why it's no longer under active development.

## References

- [A2UI](https://a2ui.org/) and its [v0.9.1 specification](https://a2ui.org/specification/v0.9.1-a2ui/)
- [AndroidX A2UI renderer](https://developer.android.com/develop/ui/compose/agentic)
- [Remote Compose architecture](https://github.com/androidx/androidx/blob/androidx-main/compose/remote/remote-core/doc/REMOTE_COMPOSE_ARCHITECTURE.md)
- [Redwood](https://github.com/cashapp/redwood)
