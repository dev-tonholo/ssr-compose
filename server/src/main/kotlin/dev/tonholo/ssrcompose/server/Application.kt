package dev.tonholo.ssrcompose.server

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

private const val DEFAULT_PORT = 8080

fun main() {
    embeddedServer(factory = Netty, port = DEFAULT_PORT) { }.start(wait = true)
}
