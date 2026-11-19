package dev.tonholo.ssrcompose.consumer.internal.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.consumer.ScreenProblem

private const val RETRY_LABEL = "Try again"
private const val MISMATCH_MESSAGE = "This screen cannot be shown right now."
private val MessagePadding = 24.dp
private val MessageSpacing = 16.dp
private val MessageMaxWidth = 480.dp
private val MessageFontSize = 16.sp
private val RetryPadding = 12.dp
private val RetryBorderWidth = 1.dp

/**
 * The plain English message `SsrScreen` shows when the app supplies no message of its own.
 * It centres the message and its retry button in the space [modifier] gives it.
 */
@Composable
internal fun DefaultScreenProblem(problem: ScreenProblem, retry: () -> Unit, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val foreground = if (dark) Color.White else Color.Black
    val style = TextStyle(color = foreground, fontSize = MessageFontSize, textAlign = TextAlign.Center)
    Box(
        modifier = modifier.background(color = if (dark) Color.Black else Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MessageMaxWidth)
                .padding(all = MessagePadding),
            verticalArrangement = Arrangement.spacedBy(space = MessageSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(text = problem.message(), style = style)
            BasicText(
                text = RETRY_LABEL,
                style = style,
                modifier = Modifier
                    .border(width = RetryBorderWidth, color = foreground)
                    .clickable(role = Role.Button, onClick = retry)
                    .padding(all = RetryPadding),
            )
        }
    }
}

private fun ScreenProblem.message(): String = when (this) {
    is ScreenProblem.Refused -> reason
    is ScreenProblem.Unavailable -> reason
    is ScreenProblem.Mismatch -> MISMATCH_MESSAGE
}
