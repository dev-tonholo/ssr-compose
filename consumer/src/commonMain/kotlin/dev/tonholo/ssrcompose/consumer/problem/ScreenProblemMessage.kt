package dev.tonholo.ssrcompose.consumer.problem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import dev.tonholo.ssrcompose.consumer.ScreenProblem
import dev.tonholo.ssrcompose.consumer.internal.screen.problemTexts

/**
 * The message `SsrScreen` shows for a [ScreenProblem] when the app supplies no `unavailable` of its own, with a retry
 * button. It centres them in the space [modifier] gives it, and arranges nothing else.
 *
 * Its texts come from `LocalSsrComposeStringResourceManager`, except the reason of [ScreenProblem.Refused] and
 * [ScreenProblem.Unavailable], which is shown as the producer wrote it.
 *
 * @param problem what to tell the user.
 * @param retry called when the user presses the retry button.
 * @param colors the container and the content colors. The content color replaces the color of [textStyle].
 * @param textStyle the style of the message and of the retry label.
 * @param contentPadding the space between the edge of the message and its content.
 * @param spacing the gap between the message and the retry button.
 * @param maxContentWidth the widest the content grows, padding included.
 * @param retryContentPadding the space between the border of the retry button and its label.
 * @param retryBorderWidth the width of the border of the retry button, drawn in the content color.
 */
@Composable
public fun ScreenProblemMessage(
    problem: ScreenProblem,
    retry: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ScreenProblemMessageColors = ScreenProblemMessageDefaults.colors(),
    textStyle: TextStyle = ScreenProblemMessageDefaults.MessageTextStyle,
    contentPadding: PaddingValues = ScreenProblemMessageDefaults.ContentPadding,
    spacing: Dp = ScreenProblemMessageDefaults.Spacing,
    maxContentWidth: Dp = ScreenProblemMessageDefaults.MaxContentWidth,
    retryContentPadding: PaddingValues = ScreenProblemMessageDefaults.RetryContentPadding,
    retryBorderWidth: Dp = ScreenProblemMessageDefaults.RetryBorderWidth,
) {
    val texts = problemTexts(problem = problem)
    val style = textStyle.copy(color = colors.contentColor)
    Box(
        modifier = modifier.background(color = colors.containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxContentWidth)
                .padding(paddingValues = contentPadding),
            verticalArrangement = Arrangement.spacedBy(space = spacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(text = texts.message, style = style)
            BasicText(
                text = texts.retryLabel,
                style = style,
                modifier = Modifier
                    .border(width = retryBorderWidth, color = colors.contentColor)
                    .clickable(role = Role.Button, onClick = retry)
                    .padding(paddingValues = retryContentPadding),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ScreenProblemMessagePreview() {
    ScreenProblemMessage(problem = ScreenProblem.ConnectionEnded, retry = {})
}
