package dev.tonholo.ssrcompose.server

import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.ScreenService
import dev.tonholo.ssrcompose.protocol.ServerEndpoint
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.path
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService

class ScreenServiceRpcTest {
    @Test
    fun `given a server with one screen, when a kRPC client asks for it, then it gets the same tree`() =
        testApplication {
            // Arrange
            val expected = ColumnNode(children = emptyList())
            val service = ScreenServiceImpl(screens = mapOf(ServerEndpoint.HOME_SCREEN to { expected }))
            application { module(service = service) }
            val client = createClient {
                install(WebSockets)
                installKrpc()
            }
            val remote = client.rpc {
                url { path(ServerEndpoint.RPC_PATH) }
                rpcConfig { serialization { json() } }
            }.withService<ScreenService>()

            // Act
            val actual = remote.screen(name = ServerEndpoint.HOME_SCREEN)

            // Assert
            assertEquals(expected = expected, actual = actual)
        }

    @Test
    fun `given a running server, when a client asks for an unknown screen, then the failure names the screen`() =
        testApplication {
            // Arrange
            val service = ScreenServiceImpl(screens = emptyMap())
            application { module(service = service) }
            val client = createClient {
                install(WebSockets)
                installKrpc()
            }
            val remote = client.rpc {
                url { path(ServerEndpoint.RPC_PATH) }
                rpcConfig { serialization { json() } }
            }.withService<ScreenService>()

            // Act
            val failure = assertFailsWith<Throwable> { remote.screen(name = "nope") }

            // Assert
            assertTrue(actual = failure.message.orEmpty().contains("Unknown screen: nope"))
        }
}
