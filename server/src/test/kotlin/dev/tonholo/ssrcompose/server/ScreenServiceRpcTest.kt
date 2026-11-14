package dev.tonholo.ssrcompose.server

import app.cash.turbine.test
import dev.tonholo.ssrcompose.protocol.ActionEvent
import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.CounterCard
import dev.tonholo.ssrcompose.protocol.CounterCardProps
import dev.tonholo.ssrcompose.protocol.ReferenceNode
import dev.tonholo.ssrcompose.protocol.ScreenNode
import dev.tonholo.ssrcompose.server.screen.CounterScreen
import dev.tonholo.ssrcompose.server.screen.MutableClock
import dev.tonholo.ssrcompose.protocol.ScreenService
import dev.tonholo.ssrcompose.protocol.ServerEndpoint
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import java.time.Instant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import kotlinx.serialization.json.Json

class ScreenServiceRpcTest {
    @Test
    fun `given a server with one screen, when a kRPC client asks for it, then it gets the same tree`() =
        testApplication {
            // Arrange
            val expected = ColumnNode(children = emptyList())
            val service = ScreenServiceImpl(screens = mapOf(ServerEndpoint.HOME_SCREEN to {}))
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

    private fun counterServer(): ScreenServiceImpl {
        val clock = MutableClock(current = Instant.parse("2026-01-01T09:00:00Z"))
        return ScreenServiceImpl(screens = mapOf(ServerEndpoint.COUNTER_SCREEN to { CounterScreen(clock = clock) }))
    }

    private fun ApplicationTestBuilder.remoteService(): ScreenService {
        val client: HttpClient = createClient {
            install(WebSockets)
            installKrpc()
        }
        return client.rpc {
            url { path(ServerEndpoint.RPC_PATH) }
            rpcConfig { serialization { json() } }
        }.withService<ScreenService>()
    }

    private fun counterProps(tree: ScreenNode): CounterCardProps {
        val reference = assertIs<ReferenceNode>(assertIs<ColumnNode>(tree).children[1])
        return Json.decodeFromJsonElement(CounterCard.propsSerializer, reference.props)
    }

    @Test
    fun `given a running server, when a client sends add one in a session, then it receives count 1`() =
        testApplication {
            // Arrange
            application { module(service = counterServer()) }
            val events = Channel<ActionEvent>(Channel.UNLIMITED)

            remoteService().session(ServerEndpoint.COUNTER_SCREEN, events.receiveAsFlow()).test {
                val first = counterProps(awaitItem())

                // Act
                events.send(ActionEvent(actionId = first.onAddOne))

                // Assert
                assertEquals(expected = 1, actual = counterProps(awaitItem()).count)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given two sessions, when one sends an action, then the other still shows count 0`() = testApplication {
        // Arrange
        application { module(service = counterServer()) }
        val remote = remoteService()
        val sendingEvents = Channel<ActionEvent>(Channel.UNLIMITED)
        val quietEvents = Channel<ActionEvent>(Channel.UNLIMITED)

        remote.session(ServerEndpoint.COUNTER_SCREEN, quietEvents.receiveAsFlow()).test {
            assertEquals(expected = 0, actual = counterProps(awaitItem()).count)
            remote.session(ServerEndpoint.COUNTER_SCREEN, sendingEvents.receiveAsFlow()).test {
                val first = counterProps(awaitItem())

                // Act
                sendingEvents.send(ActionEvent(actionId = first.onAddOne))

                // Assert
                assertEquals(expected = 1, actual = counterProps(awaitItem()).count)
                cancelAndIgnoreRemainingEvents()
            }
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given a running server, when a client opens a session for an unknown screen, then the flow fails`() =
        testApplication {
            // Arrange
            application { module(service = counterServer()) }
            val events = Channel<ActionEvent>(Channel.UNLIMITED)

            remoteService().session("nope", events.receiveAsFlow()).test {
                // Act
                val failure = awaitError()

                // Assert
                assertTrue(actual = failure.message.orEmpty().contains("Unknown screen: nope"))
            }
        }
}
