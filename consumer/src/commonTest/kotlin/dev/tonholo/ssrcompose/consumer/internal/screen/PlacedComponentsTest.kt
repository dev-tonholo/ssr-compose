package dev.tonholo.ssrcompose.consumer.internal.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.tonholo.ssrcompose.consumer.ComponentBindings
import dev.tonholo.ssrcompose.consumer.PlacedComponentScope
import dev.tonholo.ssrcompose.consumer.testing.composeAndRecompose
import dev.tonholo.ssrcompose.consumer.testing.composeOnce
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import dev.tonholo.ssrcompose.core.message.ScreenContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

private const val FIRST = "First"
private const val SECOND = "Second"
private const val THIRD = "Third"
private const val UNKNOWN = "Unknown"
private const val PARAMETER = "title"
private const val EARLY_POSITION = 1
private const val LATE_POSITION = 3
private const val LARGE_SCREEN_COMPONENTS = 10_000
private const val CHANGED_POSITION = 4_321

private fun componentsOf(vararg names: String) = names.map { PlacedComponent(name = it) }

private fun contentOf(vararg names: String) = ScreenContent.Components(components = componentsOf(*names))

private val FOUR_POSITIONS = contentOf(SECOND, FIRST, SECOND, THIRD)

private fun titled(name: String, title: String) = PlacedComponent(
    name = name,
    values = mapOf(PARAMETER to DataValue.Text(value = title)),
)

private const val CHANGED_TITLE = "Changed"

private val largeScreenReads = mutableListOf<String>()

class PlacedComponentsTest {
    @Test
    fun `given placed components, when they compose, then each binding runs from top to bottom`() {
        // Arrange
        val composed = mutableListOf<String>()
        val bindings = ComponentBindings {
            listOf(FIRST, SECOND, THIRD).forEach { name -> bind(name) { composed += name } }
        }

        // Act
        composeOnce {
            PlacedComponents(
                components = componentsOf(THIRD, FIRST, SECOND),
                bindings = bindings,
                problems = PlacedProblems(),
            )
        }

        // Assert
        assertEquals(expected = listOf(THIRD, FIRST, SECOND), actual = composed)
    }

    @Test
    fun `given two components that fail, when they compose, then the first problem is the topmost one`() {
        // Arrange
        val problems = PlacedProblems()
        val bindings = ComponentBindings {
            bind(FIRST) { }
            bind(SECOND) { placed -> placed.text(name = PARAMETER) }
            bind(THIRD) { placed -> placed.text(name = PARAMETER) }
        }

        // Act
        composeOnce {
            PlacedComponents(components = componentsOf(FIRST, SECOND, THIRD), bindings = bindings, problems = problems)
        }

        // Assert
        assertEquals(expected = SECOND, actual = problems.firstComponent(content = contentOf(FIRST, SECOND, THIRD)))
    }

    @Test
    fun `given a problem lower down, when a higher component reports later, then the higher one is first`() {
        // Arrange
        val problems = PlacedProblems()

        // Act
        problems.reporterAt(position = LATE_POSITION).report(placed = PlacedComponent(name = THIRD))
        problems.reporterAt(position = EARLY_POSITION).report(placed = PlacedComponent(name = FIRST))

        // Assert
        assertEquals(expected = FIRST, actual = problems.firstComponent(content = FOUR_POSITIONS))
    }

    @Test
    fun `given a problem higher up, when a lower component reports later, then the higher one stays first`() {
        // Arrange
        val problems = PlacedProblems()

        // Act
        problems.reporterAt(position = EARLY_POSITION).report(placed = PlacedComponent(name = FIRST))
        problems.reporterAt(position = LATE_POSITION).report(placed = PlacedComponent(name = THIRD))

        // Assert
        assertEquals(expected = FIRST, actual = problems.firstComponent(content = FOUR_POSITIONS))
    }

    @Test
    fun `given a failed component, when a screen without it arrives, then there is no problem`() {
        // Arrange
        val problems = PlacedProblems()
        val bindings = ComponentBindings { bind(FIRST) { placed -> placed.text(name = PARAMETER) } }
        val healthy = listOf(titled(name = FIRST, title = "Fixed"))
        var components by mutableStateOf(componentsOf(FIRST))

        // Act
        composeAndRecompose(
            content = { PlacedComponents(components = components, bindings = bindings, problems = problems) },
            interact = { recompose ->
                components = healthy
                recompose()
            },
        )

        // Assert
        assertNull(actual = problems.firstComponent(content = ScreenContent.Components(components = healthy)))
    }

    @Test
    fun `given a failed component, when a screen with an equal one arrives, then the problem stays`() {
        // Arrange
        val problems = PlacedProblems()
        val bindings = ComponentBindings { bind(FIRST) { placed -> placed.text(name = PARAMETER) } }
        val equalComponents = componentsOf(FIRST)
        var components by mutableStateOf(componentsOf(FIRST))

        // Act
        composeAndRecompose(
            content = { PlacedComponents(components = components, bindings = bindings, problems = problems) },
            interact = { recompose ->
                components = equalComponents
                recompose()
            },
        )

        // Assert
        assertEquals(
            expected = FIRST,
            actual = problems.firstComponent(content = ScreenContent.Components(components = equalComponents)),
        )
    }

    @Test
    fun `given no failed read, when the components compose, then there is no problem`() {
        // Arrange
        val problems = PlacedProblems()
        val bindings = ComponentBindings { bind(FIRST) { } }

        // Act
        composeOnce {
            PlacedComponents(components = componentsOf(FIRST), bindings = bindings, problems = problems)
        }

        // Assert
        assertNull(actual = problems.firstComponent(content = contentOf(FIRST)))
    }

    @Test
    fun `given a component with no binding, when it composes, then it is the problem`() {
        // Arrange
        val problems = PlacedProblems()
        val bindings = ComponentBindings { bind(FIRST) { } }

        // Act
        composeOnce {
            PlacedComponents(components = componentsOf(FIRST, UNKNOWN), bindings = bindings, problems = problems)
        }

        // Assert
        assertEquals(expected = UNKNOWN, actual = problems.firstComponent(content = contentOf(FIRST, UNKNOWN)))
    }

    @Test
    fun `given another component at a position, when a new screen arrives, then remembered state is not shared`() {
        // Arrange
        var nextIdentity = 0
        val identities = mutableMapOf<String, Int>()
        val sharedContent: @Composable (PlacedComponentScope) -> Unit = { placed ->
            identities[placed.placed.name] = remember { nextIdentity++ }
        }
        val bindings = ComponentBindings {
            bind(FIRST, sharedContent)
            bind(SECOND, sharedContent)
        }
        var components by mutableStateOf(componentsOf(FIRST))

        // Act
        composeAndRecompose(
            content = { PlacedComponents(components = components, bindings = bindings, problems = PlacedProblems()) },
            interact = { recompose ->
                components = componentsOf(SECOND)
                recompose()
            },
        )

        // Assert
        assertNotEquals(illegal = identities[FIRST], actual = identities[SECOND])
    }

    @Test
    fun `given the same component with new values, when a new screen arrives, then remembered state stays`() {
        // Arrange
        var created = 0
        val bindings = ComponentBindings {
            bind(FIRST) { placed ->
                remember { created++ }
                placed.text(name = PARAMETER)
            }
        }
        var components by mutableStateOf(listOf(titled(name = FIRST, title = "Old")))

        // Act
        composeAndRecompose(
            content = { PlacedComponents(components = components, bindings = bindings, problems = PlacedProblems()) },
            interact = { recompose ->
                components = listOf(titled(name = FIRST, title = "New"))
                recompose()
            },
        )

        // Assert
        assertEquals(expected = 1, actual = created)
    }

    @Test
    fun `given thousands of components, when one changes, then only its binding composes again`() {
        // Arrange
        largeScreenReads.clear()
        val bindings = ComponentBindings {
            bind(FIRST) { placed -> largeScreenReads += placed.text(name = PARAMETER) }
        }
        val initial = List(size = LARGE_SCREEN_COMPONENTS) { index -> titled(name = FIRST, title = "Item$index") }
        var components by mutableStateOf(initial)
        var firstReads = emptyList<String>()
        val problems = PlacedProblems()

        // Act
        composeAndRecompose(
            content = { PlacedComponents(components = components, bindings = bindings, problems = problems) },
            interact = { recompose ->
                firstReads = largeScreenReads.toList()
                largeScreenReads.clear()
                components = initial.mapIndexed { index, component ->
                    if (index == CHANGED_POSITION) titled(name = FIRST, title = CHANGED_TITLE) else component.copy()
                }
                recompose()
            },
        )

        // Assert
        assertEquals(expected = List(size = LARGE_SCREEN_COMPONENTS) { index -> "Item$index" }, actual = firstReads)
        assertEquals(expected = listOf(CHANGED_TITLE), actual = largeScreenReads)
    }
}
