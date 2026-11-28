package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.tonholo.ssrcompose.core.document.DocumentNode
import dev.tonholo.ssrcompose.producer.ScaledBasicText
import dev.tonholo.ssrcompose.producer.internal.ScreenFrame
import dev.tonholo.ssrcompose.producer.internal.scene.SceneSetup

private const val SHOWN_TEXT_SIZE_SP = 14

/** A text a test screen shows, in the bundled Roboto so the reader can send it. */
@Composable
internal fun ShownText(text: String) {
    ScaledBasicText(text = text, style = robotoStyle(sizeSp = SHOWN_TEXT_SIZE_SP))
}

internal fun liveSetup(layoutDirection: LayoutDirection = LayoutDirection.Ltr): SceneSetup = SceneSetup(
    widthPx = DEFAULT_SCENE_WIDTH_PX,
    heightPx = DEFAULT_SCENE_HEIGHT_PX,
    density = Density(density = 1f, fontScale = LIVE_FONT_SCALE),
    layoutDirection = layoutDirection,
)

/** The first text of a sent frame, in document order. */
internal fun ScreenFrame.sentText(): DocumentNode.Text = (this as ScreenFrame.Sent).document.root.firstText()

/** Where the first text of a sent frame starts, from the left. */
internal fun ScreenFrame.textX(): Float = sentText().x

/** A sent frame as its revision and the texts it shows, in document order. */
internal data class ShownFrame(val revision: Long, val texts: List<String>)

internal fun ScreenFrame.shown(): ShownFrame {
    val sent = this as ScreenFrame.Sent
    return ShownFrame(revision = sent.revision, texts = sent.document.root.texts())
}

private fun DocumentNode.texts(): List<String> = when (this) {
    is DocumentNode.Text -> listOf(text)
    is DocumentNode.Box -> children.flatMap { it.texts() }
    is DocumentNode.Scroll -> children.flatMap { it.texts() }
    is DocumentNode.Switcher -> variants[selected].flatMap { it.texts() }
    is DocumentNode.Component -> emptyList()
}
