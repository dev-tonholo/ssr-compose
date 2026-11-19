package dev.tonholo.ssrcompose.consumer.problem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse

/**
 * The colors of a [ScreenProblemMessage]: the [containerColor] behind it, and the [contentColor] of its text and of
 * the border of its retry button. Choose them as a readable pair. Build the default pair with
 * [ScreenProblemMessageDefaults.colors].
 */
@Immutable
public class ScreenProblemMessageColors(
    public val containerColor: Color,
    public val contentColor: Color,
) {
    /** Returns a copy that takes each given color, and keeps the current one for a [Color.Unspecified] argument. */
    public fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
    ): ScreenProblemMessageColors = ScreenProblemMessageColors(
        containerColor = containerColor.takeOrElse { this.containerColor },
        contentColor = contentColor.takeOrElse { this.contentColor },
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScreenProblemMessageColors) return false
        return containerColor == other.containerColor && contentColor == other.contentColor
    }

    override fun hashCode(): Int = 31 * containerColor.hashCode() + contentColor.hashCode()
}
