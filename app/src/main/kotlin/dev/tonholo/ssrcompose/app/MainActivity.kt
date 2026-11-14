package dev.tonholo.ssrcompose.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.tonholo.ssrcompose.app.data.createScreenService
import dev.tonholo.ssrcompose.app.presentation.ScreenRoute
import dev.tonholo.ssrcompose.app.presentation.ScreenViewModel
import dev.tonholo.ssrcompose.app.presentation.ui.components.ComponentRegistry
import dev.tonholo.ssrcompose.app.presentation.ui.components.GreetingCard
import dev.tonholo.ssrcompose.protocol.GreetingCard as GreetingCardContract
import dev.tonholo.ssrcompose.protocol.ServerEndpoint
import kotlinx.coroutines.Dispatchers

private const val EMULATOR_HOST_LOOPBACK = "10.0.2.2"

class MainActivity : ComponentActivity() {
    private val registry = ComponentRegistry().apply {
        register(GreetingCardContract) { props -> GreetingCard(props = props) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel = viewModel {
                    ScreenViewModel(
                        service = createScreenService(host = EMULATOR_HOST_LOOPBACK),
                        screenName = ServerEndpoint.HOME_SCREEN,
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
