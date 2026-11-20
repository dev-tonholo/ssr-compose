package dev.tonholo.ssrcompose.consumer.internal.screen

import androidx.compose.runtime.mutableStateMapOf
import dev.tonholo.ssrcompose.consumer.internal.read.PlacedProblemReporter
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import dev.tonholo.ssrcompose.core.message.ScreenContent

/**
 * The placed components of a shown screen that could not be drawn, by position.
 * A report is a snapshot write made during composition. The runtime applies it with that composition, also when the
 * composition then throws, so a read that reports just before it throws is still seen.
 */
internal class PlacedProblems {
    private val failed = mutableStateMapOf<Int, PlacedComponent>()

    /** The name of the topmost component of [content] with a problem, or null when all of them can be drawn. */
    fun firstComponent(content: ScreenContent): String? = when (content) {
        is ScreenContent.Components -> firstComponent(components = content.components)
    }

    private fun firstComponent(components: List<PlacedComponent>): String? = failed.entries
        .filter { (position, placed) -> components.getOrNull(index = position) == placed }
        .minByOrNull { (position, _) -> position }
        ?.value
        ?.name

    fun reporterAt(position: Int) = PlacedProblemReporter { placed -> failed[position] = placed }
}
