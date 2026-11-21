package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.core.document.DocumentNode
import dev.tonholo.ssrcompose.core.document.ScreenDocument
import dev.tonholo.ssrcompose.producer.internal.font.bundledRobotoFamily
import dev.tonholo.ssrcompose.producer.internal.reader.ScreenRead
import dev.tonholo.ssrcompose.producer.internal.reader.readScreen
import kotlin.test.assertIs

internal const val READ_DENSITY = 2f

internal fun readSceneOf(
    spec: SceneSpec = SceneSpec(density = Density(density = READ_DENSITY)),
    content: @Composable () -> Unit,
): ScreenRead = withRenderedScene(spec = spec, content = content) { scene ->
    scene.read { roots ->
        readScreen(roots = roots, density = scene.density, fontScaleCurve = scene.fontScaleCurve)
    }
}

internal fun documentOf(
    spec: SceneSpec = SceneSpec(density = Density(density = READ_DENSITY)),
    content: @Composable () -> Unit,
): ScreenDocument = assertIs<ScreenRead.Document>(value = readSceneOf(spec = spec, content = content)).document

internal fun robotoStyle(sizeSp: Int, weight: FontWeight = FontWeight.Normal): TextStyle =
    TextStyle(fontFamily = bundledRobotoFamily(), fontSize = sizeSp.sp, fontWeight = weight)

/** Semantics ids are unique in the whole process, so tests compare documents with every id set to 0. */
internal fun DocumentNode.withoutIds(): DocumentNode = when (this) {
    is DocumentNode.Box -> copy(id = 0, children = children.map { it.withoutIds() })
    is DocumentNode.Text -> copy(id = 0)
    is DocumentNode.Scroll -> copy(id = 0, children = children.map { it.withoutIds() })
    is DocumentNode.Switcher -> copy(id = 0, variants = variants.map { variant -> variant.map { it.withoutIds() } })
    is DocumentNode.Component -> copy(id = 0)
}
