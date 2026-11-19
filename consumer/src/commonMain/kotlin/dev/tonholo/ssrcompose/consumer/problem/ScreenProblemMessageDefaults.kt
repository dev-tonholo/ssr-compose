package dev.tonholo.ssrcompose.consumer.problem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DefaultMessageFontSize = 16.sp
private val DefaultContentPadding = 24.dp
private val DefaultSpacing = 16.dp
private val DefaultMaxContentWidth = 480.dp
private val DefaultRetryContentPadding = 12.dp
private val DefaultRetryBorderWidth = 1.dp

/** The values [ScreenProblemMessage] uses for each style parameter the app does not set. */
public object ScreenProblemMessageDefaults {
    /** The text style of the message and of the retry label. Its color is replaced by the content color. */
    public val MessageTextStyle: TextStyle = TextStyle(fontSize = DefaultMessageFontSize, textAlign = TextAlign.Center)

    /** The space between the edge of the message and the content. */
    public val ContentPadding: PaddingValues = PaddingValues(all = DefaultContentPadding)

    /** The gap between the message and the retry button. */
    public val Spacing: Dp = DefaultSpacing

    /** The widest the message and the retry button grow, padding included. */
    public val MaxContentWidth: Dp = DefaultMaxContentWidth

    /** The space between the border of the retry button and its label. */
    public val RetryContentPadding: PaddingValues = PaddingValues(all = DefaultRetryContentPadding)

    /** The width of the border of the retry button. */
    public val RetryBorderWidth: Dp = DefaultRetryBorderWidth

    /**
     * Builds the colors of a [ScreenProblemMessage]. A color left as [Color.Unspecified] takes the default for the
     * system theme: black on white in light theme, and white on black in dark theme.
     */
    @Composable
    public fun colors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
    ): ScreenProblemMessageColors = ScreenProblemMessageColors(
        containerColor = containerColor.takeOrElse { if (isSystemInDarkTheme()) Color.Black else Color.White },
        contentColor = contentColor.takeOrElse { if (isSystemInDarkTheme()) Color.White else Color.Black },
    )
}
