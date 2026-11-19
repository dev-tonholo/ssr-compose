package dev.tonholo.ssrcompose.consumer.problem

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import dev.tonholo.ssrcompose.consumer.testing.InSystemTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

private val GIVEN_CONTAINER = Color(color = 0xFFD81B60)
private val GIVEN_CONTENT = Color(color = 0xFF00897B)
private const val COMPOSITIONS_BEFORE_THE_THEME_CHANGES = 1
private const val COMPOSITIONS_AFTER_THE_THEME_CHANGES = 2

class ScreenProblemMessageDefaultColorsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var compositions = 0
    private var latest: ScreenProblemMessageColors? = null

    @Test
    fun given_explicit_colors__when_colors_are_built__then_they_are_returned_unchanged() {
        // Arrange
        composeRule.setContent { ColorsProbe(containerColor = GIVEN_CONTAINER, contentColor = GIVEN_CONTENT) }

        // Act
        composeRule.waitForIdle()

        // Assert
        assertEquals(
            expected = ScreenProblemMessageColors(containerColor = GIVEN_CONTAINER, contentColor = GIVEN_CONTENT),
            actual = latest,
        )
    }

    @Test
    fun given_both_colors__when_the_system_theme_changes__then_the_colors_are_not_built_again() {
        // Arrange
        var nightMode by mutableIntStateOf(value = Configuration.UI_MODE_NIGHT_NO)
        composeRule.setContent {
            InSystemTheme(nightMode = nightMode) {
                ColorsProbe(containerColor = GIVEN_CONTAINER, contentColor = GIVEN_CONTENT)
            }
        }

        // Act
        composeRule.runOnIdle { nightMode = Configuration.UI_MODE_NIGHT_YES }
        composeRule.waitForIdle()

        // Assert
        assertEquals(expected = COMPOSITIONS_BEFORE_THE_THEME_CHANGES, actual = compositions)
    }

    @Test
    fun given_an_unspecified_color__when_the_system_theme_changes__then_the_colors_are_built_again() {
        // Arrange
        var nightMode by mutableIntStateOf(value = Configuration.UI_MODE_NIGHT_NO)
        composeRule.setContent {
            InSystemTheme(nightMode = nightMode) {
                ColorsProbe(containerColor = Color.Unspecified, contentColor = GIVEN_CONTENT)
            }
        }

        // Act
        composeRule.runOnIdle { nightMode = Configuration.UI_MODE_NIGHT_YES }
        composeRule.waitForIdle()

        // Assert
        assertEquals(expected = COMPOSITIONS_AFTER_THE_THEME_CHANGES, actual = compositions)
        assertEquals(expected = Color.Black, actual = latest?.containerColor)
    }

    @Composable
    private fun ColorsProbe(containerColor: Color, contentColor: Color) {
        val colors = ScreenProblemMessageDefaults.colors(containerColor = containerColor, contentColor = contentColor)
        SideEffect {
            compositions++
            latest = colors
        }
    }
}
