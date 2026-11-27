package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable

/** A box as wide as it may be that places its one child the way [place] says. */
@Composable
internal fun PlacedBy(content: @Composable () -> Unit, place: Placeable.PlacementScope.(Placeable) -> Unit) {
    Layout(content = content) { measurables, constraints ->
        val placeable = measurables.single().measure(constraints)
        layout(width = constraints.maxWidth, height = placeable.height) { place(placeable) }
    }
}
