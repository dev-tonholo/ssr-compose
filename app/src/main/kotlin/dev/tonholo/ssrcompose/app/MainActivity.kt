package dev.tonholo.ssrcompose.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import dev.tonholo.ssrcompose.app.data.createScreenService
import dev.tonholo.ssrcompose.protocol.ServerEndpoint

private const val EMULATOR_HOST_LOOPBACK = "10.0.2.2"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val service = createScreenService(host = EMULATOR_HOST_LOOPBACK)
        setContent {
            MaterialTheme {
                val result by produceState(initialValue = "Loading") {
                    value = runCatching { service.screen(name = ServerEndpoint.HOME_SCREEN) }
                        .fold(
                            onSuccess = { node -> node::class.simpleName.orEmpty() },
                            onFailure = { error -> "Error: $error" },
                        )
                }
                Scaffold { contentPadding ->
                    Box(modifier = Modifier.padding(paddingValues = contentPadding)) { Text(text = result) }
                }
            }
        }
    }
}
