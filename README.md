# ssr-compose

Server-side rendering for Jetpack Compose. A server runs composable logic and sends the result to a thin client. The client renders shared composables natively and plays the parts it does not have.

The library is being built here. Nothing is released yet.

## Proof of concept

The idea was proven first in [poc](poc). It is a separate Gradle project with a Ktor server and an Android app, kept as a reference. Its [README](poc/README.md) explains what was proven, the limits found, and how to run it.
