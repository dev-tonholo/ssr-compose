package dev.tonholo.ssrcompose.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tonholo.ssrcompose.app.presentation.ui.Dimens
import dev.tonholo.ssrcompose.app.presentation.ui.components.ComponentRegistry
import dev.tonholo.ssrcompose.app.presentation.ui.components.ScreenRenderer

@Composable
fun ScreenRoute(viewModel: ScreenViewModel, registry: ComponentRegistry, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        ScreenUiState.Loading -> Centered(modifier = modifier) { CircularProgressIndicator() }
        is ScreenUiState.Loaded -> ScreenRenderer(node = current.tree, registry = registry, modifier = modifier)
        is ScreenUiState.Failed -> Centered(modifier = modifier) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = Dimens.ItemSpacing),
            ) {
                Text(text = current.message)
                Button(onClick = { viewModel.onEvent(event = ScreenEvent.Retry) }) { Text(text = "Retry") }
            }
        }
    }
}

@Composable
private fun Centered(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
