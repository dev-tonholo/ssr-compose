package dev.tonholo.ssrcompose.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import dev.tonholo.ssrcompose.app.data.createScreenService
import dev.tonholo.ssrcompose.app.presentation.ui.components.FragmentPlayer
import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.FragmentNode
import dev.tonholo.ssrcompose.protocol.ScreenNode
import dev.tonholo.ssrcompose.protocol.ServerEndpoint

private const val BANNER_ASPECT_RATIO = 600f / 160f
private const val EMULATOR_HOST_LOOPBACK = "10.0.2.2"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val service = createScreenService(host = EMULATOR_HOST_LOOPBACK)
        setContent {
            MaterialTheme {
                val result by produceState<Result<ScreenNode>?>(initialValue = null) {
                    value = runCatching { service.screen(name = ServerEndpoint.HOME_SCREEN) }
                }
                Scaffold { contentPadding ->
                    Box(modifier = Modifier.padding(paddingValues = contentPadding)) {
                        val screen = result
                        val fragment = screen?.getOrNull()?.let { node -> node.firstFragment() }
                        when {
                            screen == null -> Text(text = "Loading")
                            fragment != null -> FragmentPlayer(
                                document = fragment.document,
                                modifier = Modifier.fillMaxWidth().aspectRatio(ratio = BANNER_ASPECT_RATIO),
                            )
                            else -> Text(text = "Error: ${screen.exceptionOrNull() ?: "no fragment"}")
                        }
                    }
                }
            }
        }
    }
}

private fun ScreenNode.firstFragment(): FragmentNode? =
    (this as? ColumnNode)?.children?.filterIsInstance<FragmentNode>()?.firstOrNull()
