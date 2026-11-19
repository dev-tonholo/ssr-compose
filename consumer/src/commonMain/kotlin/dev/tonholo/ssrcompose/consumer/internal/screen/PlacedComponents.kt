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
    Column(modifier = modifier) {
        PlacedComponents(content = content, bindings = bindings, problems = problems)
    }
}

/**
 * Composes the binding of each placed component of [content], in order. A component with no binding, or one whose
 * binding cannot read a value, is reported to [problems].
 */
@Composable
internal fun PlacedComponents(content: ScreenContent, bindings: ComponentBindings, problems: PlacedProblems) {
    content.components.forEachIndexed { position, placed ->
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
