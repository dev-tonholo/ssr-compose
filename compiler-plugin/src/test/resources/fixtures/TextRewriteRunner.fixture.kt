@file:JvmName("TextRewriteRunner")
@file:OptIn(InternalComposeUiApi::class)

package app.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.core.handshake.FontScaleCurve
import dev.tonholo.ssrcompose.core.handshake.FontScalePoint
import dev.tonholo.ssrcompose.producer.internal.font.bundledRobotoFamily
import dev.tonholo.ssrcompose.producer.internal.text.LocalScaledTextScene
import dev.tonholo.ssrcompose.producer.internal.text.ScaledTextScene
import dev.tonholo.ssrcompose.producer.internal.text.scaledTextMark

private const val SCENE_WIDTH_PX = 1000
private const val SCENE_HEIGHT_PX = 2000
private const val DENSITY = 2.5f
private const val FONT_SCALE = 1.3f
private const val SMALL_SP = 16
private const val LARGE_SP = 24
private const val SAMPLE = "Served by the producer"
private const val NO_MARK = "none"

private val emulatorCurve = FontScaleCurve(
    points = listOf(
        FontScalePoint(sp = 8f, dp = 10.4f),
        FontScalePoint(sp = 10f, dp = 13f),
        FontScalePoint(sp = 12f, dp = 15.6f),
        FontScalePoint(sp = 14f, dp = 18.8f),
        FontScalePoint(sp = 18f, dp = 21.6f),
        FontScalePoint(sp = 20f, dp = 23.2f),
        FontScalePoint(sp = 24f, dp = 26.4f),
        FontScalePoint(sp = 30f, dp = 33f),
        FontScalePoint(sp = 100f, dp = 110f),
    ),
)

private fun style(sizeSp: Int) = TextStyle(fontFamily = bundledRobotoFamily(), fontSize = sizeSp.sp)

/** Draws [screen] at density 2.5 and font scale 1.3 and describes each text as "height in px,mark size in sp". */
fun probe(screen: String, withCurve: Boolean): String {
    val scene = ImageComposeScene(
        width = SCENE_WIDTH_PX,
        height = SCENE_HEIGHT_PX,
        density = Density(density = DENSITY, fontScale = FONT_SCALE),
    )
    try {
        scene.setContent {
            if (withCurve) {
                CompositionLocalProvider(
                    LocalScaledTextScene provides ScaledTextScene(curve = emulatorCurve, fontScale = FONT_SCALE),
                ) { ScreenNamed(screen) }
            } else {
                ScreenNamed(screen)
            }
        }
        scene.render().close()
        return scene.semanticsOwners
            .flatMap { owner -> owner.unmergedRootSemanticsNode.texts() }
            .joinToString(separator = ";") { node -> "${node.size.height},${node.markSize()}" }
    } finally {
        scene.close()
    }
}

private fun SemanticsNode.texts(): List<SemanticsNode> =
    (if (SemanticsProperties.Text in config) listOf(this) else emptyList()) + children.flatMap { it.texts() }

private fun SemanticsNode.markSize(): String =
    config.getOrNull(scaledTextMark)?.sizeSp?.toString() ?: NO_MARK

@Composable
private fun ScreenNamed(screen: String) {
    when (screen) {
        "stringPositional" -> StringPositional()
        "annotatedString" -> AnnotatedStringText()
        "namedOutOfOrder" -> NamedOutOfOrder()
        "defaultsLeftOut" -> DefaultsLeftOut()
        "insideComposable" -> InsideComposable()
        "insideLambda" -> InsideLambda()
        "insideInlineFunction" -> InsideInlineFunction()
        "functionReference" -> FunctionReference()
        else -> error("No screen named $screen")
    }
}

@Composable
private fun StringPositional() {
    Column {
        BasicText(SAMPLE, Modifier, style(SMALL_SP))
        BasicText(SAMPLE, Modifier, style(LARGE_SP))
    }
}

@Composable
private fun AnnotatedStringText() {
    Column {
        BasicText(AnnotatedString(SAMPLE), Modifier, style(SMALL_SP))
        BasicText(AnnotatedString(SAMPLE), Modifier, style(LARGE_SP))
    }
}

@Composable
private fun NamedOutOfOrder() {
    Column {
        BasicText(style = style(SMALL_SP), text = SAMPLE, maxLines = 1, modifier = Modifier)
        BasicText(maxLines = 1, style = style(LARGE_SP), modifier = Modifier, text = SAMPLE)
    }
}

@Composable
private fun DefaultsLeftOut() {
    Column {
        BasicText(SAMPLE, style = style(SMALL_SP))
        BasicText(SAMPLE, style = style(LARGE_SP))
    }
}

@Composable
private fun Label(sizeSp: Int) {
    BasicText(text = SAMPLE, style = style(sizeSp))
}

@Composable
private fun InsideComposable() {
    Column {
        Label(SMALL_SP)
        Label(LARGE_SP)
    }
}

@Composable
private fun Holder(content: @Composable () -> Unit) {
    Column { content() }
}

@Composable
private fun InsideLambda() {
    Holder {
        BasicText(text = SAMPLE, style = style(SMALL_SP))
        BasicText(text = SAMPLE, style = style(LARGE_SP))
    }
}

@Composable
private inline fun InlineLabel(sizeSp: Int) {
    BasicText(text = SAMPLE, style = style(sizeSp))
}

@Composable
private fun InsideInlineFunction() {
    Column {
        InlineLabel(SMALL_SP)
        InlineLabel(LARGE_SP)
    }
}

@Composable
private fun FunctionReference() {
    val draw: @Composable (String) -> Unit = ::BasicText
    draw(SAMPLE)
}
