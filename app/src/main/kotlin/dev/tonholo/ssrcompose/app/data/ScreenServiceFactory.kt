package dev.tonholo.ssrcompose.app.data

import dev.tonholo.ssrcompose.protocol.ActionEvent
import dev.tonholo.ssrcompose.protocol.ScreenNode
import dev.tonholo.ssrcompose.protocol.ScreenService
import dev.tonholo.ssrcompose.protocol.ServerEndpoint
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.URLProtocol
import io.ktor.http.path
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.ktor.client.KtorRpcClient
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService

private const val PING_INTERVAL_MILLIS = 2_000L

fun createScreenService(host: String): ScreenService = ReconnectingScreenService(host = host)

private class ReconnectingScreenService(private val host: String) : ScreenService {
    override suspend fun screen(name: String): ScreenNode {
        val client = newClient()
        try {
            return connect(client).withService<ScreenService>().screen(name = name)
        } finally {
            client.close()
        }
    }

    override fun session(name: String, events: Flow<ActionEvent>): Flow<ScreenNode> = channelFlow {
        val client = newClient()
        try {
            val rpcClient = connect(client)
            launch { closeWhenConnectionEnds(rpcClient) }
            rpcClient.withService<ScreenService>()
                .session(name = name, events = events)
                .collect { tree -> send(tree) }
        } finally {
            client.close()
        }
    }

    private suspend fun ProducerScope<ScreenNode>.closeWhenConnectionEnds(rpcClient: KtorRpcClient) {
        rpcClient.webSocketSession.await().coroutineContext.job.join()
        close()
    }

    private fun newClient(): HttpClient = HttpClient(engineFactory = OkHttp) {
        engine {
            config { pingInterval(PING_INTERVAL_MILLIS, TimeUnit.MILLISECONDS) }
        }
        install(WebSockets)
        installKrpc()
    }

    private suspend fun connect(client: HttpClient) = client.rpc {
        url {
            protocol = URLProtocol.WS
            host = this@ReconnectingScreenService.host
            port = ServerEndpoint.PORT
            path(ServerEndpoint.RPC_PATH)
        }
        rpcConfig { serialization { json() } }
    }
}
