package dev.tonholo.ssrcompose.consumer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import dev.tonholo.ssrcompose.consumer.testing.composeAndRecompose
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import kotlin.test.Test
import kotlin.test.assertEquals

private const val GREETING = "Greeting"
private const val TITLE = "title"
private const val FIRST_TEXT = "Pay now"
private const val SECOND_TEXT = "Pay later"
private const val FIRST_RENDER = 1
private const val SECOND_RENDER = 2

private fun greeting(text: String) =
    PlacedComponent(name = GREETING, values = mapOf(TITLE to DataValue.Text(value = text)))

class PlacedComponentScopeSkippingTest {
    private var renders = 0

    @Composable
    private fun CountRenders(scope: PlacedComponentScope) {
        renders++
        scope.text(name = TITLE)
    }

    @Test
    fun `given an equal placed component in a new scope, when the screen recomposes, then the binding is skipped`() {
        // Arrange
        val tick = mutableIntStateOf(0)

        composeAndRecompose(
            content = {
                tick.intValue
                CountRenders(scope = PlacedComponentScope(placed = greeting(text = FIRST_TEXT)))
            },
        ) { recompose ->
            // Act
            tick.intValue += 1
            recompose()
        }

        // Assert
        assertEquals(expected = FIRST_RENDER, actual = renders)
    }

    @Test
    fun `given a changed placed component, when the screen recomposes, then the binding runs again`() {
        // Arrange
        val placed = mutableStateOf(greeting(text = FIRST_TEXT))

        composeAndRecompose(
            content = { CountRenders(scope = PlacedComponentScope(placed = placed.value)) },
        ) { recompose ->
            // Act
            placed.value = greeting(text = SECOND_TEXT)
            recompose()
        }

        // Assert
        assertEquals(expected = SECOND_RENDER, actual = renders)
    }
}
