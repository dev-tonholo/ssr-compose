package dev.tonholo.ssrcompose.consumer.internal.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.tonholo.ssrcompose.consumer.ComponentBindings
import dev.tonholo.ssrcompose.consumer.PlacedComponentScope
import dev.tonholo.ssrcompose.consumer.internal.read.PlacedProblemReporter
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import dev.tonholo.ssrcompose.core.message.ScreenContent

/**
 * The provisional Feature 1 fallback (FR-034): shows the top-level placed components of [content] top to bottom,
 * until server-rendered documents own layout.
 */
@Composable
internal fun ProvisionalScreenContent(
    content: ScreenContent,
    bindings: ComponentBindings,
    problems: PlacedProblems,
    modifier: Modifier = Modifier,
) {
    when (content) {
        is ScreenContent.Components -> Column(modifier = modifier) {
            PlacedComponents(components = content.components, bindings = bindings, problems = problems)
        }
    }
}

/**
 * Composes the binding of each of [components], in order. A component with no binding, or one whose
 * binding cannot read a value, is reported to [problems].
 */
@Composable
internal fun PlacedComponents(
    components: List<PlacedComponent>,
    bindings: ComponentBindings,
    problems: PlacedProblems,
) {
    components.forEachIndexed { position, placed ->
        key(position, placed.name) {
            val reporter = remember(problems) { problems.reporterAt(position = position) }
            PlacedBinding(placed = placed, bindings = bindings, reporter = reporter)
        }
    }
}

@Composable
private fun PlacedBinding(placed: PlacedComponent, bindings: ComponentBindings, reporter: PlacedProblemReporter) {
    val binding = bindings.find(name = placed.name)
    if (binding == null) {
        reporter.report(placed = placed)
        return
    }
    val scope = remember(placed) { PlacedComponentScope(placed = placed, problems = reporter) }
    binding(scope)
}
