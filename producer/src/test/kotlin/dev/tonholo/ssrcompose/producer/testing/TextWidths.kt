package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.sp

/**
 * The width in pixels of [text] in [family] through a resolver of its own, so no other font has ever been loaded
 * beside [family].
 */
internal fun widthAlone(text: String, family: FontFamily, sizeSp: Int): Int {
    val ownResolver = createFontFamilyResolver()
    var width: Int? = null
    withRenderedScene(
        spec = SceneSpec(widthPx = WIDE_SCENE_PX),
        content = {
            CompositionLocalProvider(LocalFontFamilyResolver provides ownResolver) {
                BasicText(
                    text = text,
                    style = TextStyle(fontFamily = family, fontSize = sizeSp.sp),
                    onTextLayout = { width = it.size.width },
                )
            }
        },
        read = {},
    )
    return checkNotNull(width) { "The text was not laid out" }
}
