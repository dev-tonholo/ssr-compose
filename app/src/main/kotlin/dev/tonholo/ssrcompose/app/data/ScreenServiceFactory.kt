package dev.tonholo.ssrcompose.app.data

import dev.tonholo.ssrcompose.protocol.ScreenService
import dev.tonholo.ssrcompose.protocol.ServerEndpoint
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.URLProtocol
import io.ktor.http.path
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService

fun createScreenService(host: String): ScreenService {
    val client = HttpClient(engineFactory = OkHttp) {
        install(WebSockets)
        installKrpc()
    }
    return client.rpc {
        url {
            protocol = URLProtocol.WS
            this.host = host
            port = ServerEndpoint.PORT
            path(ServerEndpoint.RPC_PATH)
        }
        rpcConfig { serialization { json() } }
    }.withService<ScreenService>()
}
