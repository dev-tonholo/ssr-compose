package dev.tonholo.ssrcompose.server

import dev.tonholo.ssrcompose.protocol.ScreenService
import dev.tonholo.ssrcompose.protocol.ServerEndpoint
import dev.tonholo.ssrcompose.server.screen.HomeScreen
import dev.tonholo.ssrcompose.server.tree.renderTree
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import java.time.Clock
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.registerService

fun Application.module(service: ScreenService) {
    install(Krpc)
    routing {
        rpc(ServerEndpoint.RPC_PATH) {
            rpcConfig { serialization { json() } }
            registerService<ScreenService> { service }
        }
    }
}

fun main() {
    val service = ScreenServiceImpl(
        screens = mapOf(ServerEndpoint.HOME_SCREEN to { renderTree { HomeScreen(clock = Clock.systemDefaultZone()) } }),
    )
    embeddedServer(factory = Netty, port = ServerEndpoint.PORT) { module(service = service) }
        .start(wait = true)
}
