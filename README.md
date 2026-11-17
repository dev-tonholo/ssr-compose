# ssr-compose

Server-side rendering for Jetpack Compose.

> [!NOTE]
> This project is experimental and in early development. Nothing is published to Maven yet, and there is no stable API to depend on. So far the core protocol, the catalog bundle format and the consumer bindings are built and tested, and the compiler plugin and build tooling are proven only by small experiments. The first feature is the consumer catalog and bridge, which lets a server screen place an app's own composables. It is not finished, so do not use this in an app yet. APIs will change without notice.

A server runs your composable logic: state, effects, lambdas, conditions and loops. The app shows the result. It renders the components it already has natively, and plays the parts it does not have from a document the server sends.

This is server-side rendering, not server-driven rendering. The server is not limited to arranging components the app already ships.

## Status

The library is in early development. Nothing is released yet, and there is no API to depend on.

The idea was proven first in a proof of concept, kept in [poc](poc) as a reference. Its [README](poc/README.md) explains what was proven, the limits found, and how to run it.

The root Gradle build now holds the library modules, and the first feature is being built there. It is the consumer catalog and bridge. The [spec](specs/001-consumer-catalog-bridge/spec.md), the [plan](specs/001-consumer-catalog-bridge/plan.md) and the [task list](specs/001-consumer-catalog-bridge/tasks.md) show what is planned and what is done.

What exists today:

- The core protocol: the handshake, the messages, the connection and an in-memory connection for tests.
- The catalog bundle: the model, its checks, and the reader and writer for the manifest and its parts.
- Experiments for the compiler plugin and the build tooling, with their results written in the [research notes](specs/001-consumer-catalog-bridge/research.md).
- The consumer bindings, which read the values a producer sends for a placed app composable.

What does not exist yet: a producer that serves screens, the consumer session and screen, and the Gradle tasks that tie the catalog to a build. No screen runs end to end with the new libraries. The only end-to-end demo is the proof of concept.

## What is being built

- A consumer library for the app. It connects to a producer, tells it what the app can show, and renders what comes back.
- A producer library for the server. It runs composables and turns them into something a consumer can show.
- A core library with the code both sides share: the document format, the component catalog, the handshake and the messages.
- A Kotlin compiler plugin that generates the bindings between producer and consumer, such as action IDs, so they are not written by hand.

## How it works

1. The app connects and introduces itself: its screen, theme, language, version, and the catalog of components it has.
2. The producer runs a composable for that app and keeps it alive for the session.
3. For a component the app has, the producer sends a reference with its data, and the app renders its own composable.
4. For anything else, the producer sends a document, and the app renders it.
5. A click in the app goes back to the producer and runs the lambda there. The screen updates when state changes, whether a click or a server-side effect changed it.

## Principles

The rules this project is built by are in the [constitution](.specify/memory/constitution.md). The short version:

- Real server-side rendering, with composable logic on the producer.
- Our own document format, with no dependency on AndroidX Remote Compose.
- A catalog and a handshake before anything is rendered.
- A real-time connection that recovers by itself and is always encrypted.
- A versioned producer, so apps that did not update keep working.
- Tests first, and evidence over assumption.

## Platforms

- The consumer library is a Kotlin Multiplatform library with Android as its only target for now.
- The producer library runs on a plain JVM server.
- The libraries are meant to be consumable from Gradle and from the Kotlin toolchain.

## Contributing

Contributions are welcome. Start with [CONTRIBUTING.md](CONTRIBUTING.md), and please follow the [Code of Conduct](CODE_OF_CONDUCT.md).

To report a security problem, see [SECURITY.md](SECURITY.md). Do not open a public issue for it.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
