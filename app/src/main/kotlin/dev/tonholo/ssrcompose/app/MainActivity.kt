package dev.tonholo.ssrcompose.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.tonholo.ssrcompose.app.data.createScreenService
import dev.tonholo.ssrcompose.app.presentation.ScreenRoute
import dev.tonholo.ssrcompose.app.presentation.ScreenViewModel
import dev.tonholo.ssrcompose.app.presentation.ui.components.ComponentRegistry
import dev.tonholo.ssrcompose.app.presentation.ui.components.CounterCard
import dev.tonholo.ssrcompose.app.presentation.ui.components.GreetingCard
import dev.tonholo.ssrcompose.protocol.CounterCard as CounterCardContract
import dev.tonholo.ssrcompose.protocol.GreetingCard as GreetingCardContract
import dev.tonholo.ssrcompose.protocol.ServerEndpoint
import kotlinx.coroutines.Dispatchers

private const val DEVICE_LOOPBACK_HOST = "127.0.0.1"

class MainActivity : ComponentActivity() {
    private val registry = ComponentRegistry().apply {
        register(GreetingCardContract) { props, _ -> GreetingCard(props = props) }
        register(CounterCardContract) { props, onAction -> CounterCard(props = props, onAction = onAction) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val viewModel = viewModel {
                    ScreenViewModel(
                        service = createScreenService(host = DEVICE_LOOPBACK_HOST),
                        screenName = ServerEndpoint.COUNTER_SCREEN,
                        dispatcher = Dispatchers.Default,
                    )
                }
                Scaffold { contentPadding ->
                    ScreenRoute(
                        viewModel = viewModel,
                        registry = registry,
                        modifier = Modifier.padding(paddingValues = contentPadding),
                    )
                }
            }
        }
    }
}
