package dev.tonholo.ssrcompose.contracttests.textgate

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import java.io.File

/**
 * Loads a font from the bytes inside its artifact and never from a system font.
 * [copyDirectory] holds the copies of the files that the text engine reads.
 */
class FontFileLoader(
    private val copyDirectory: File,
    private val classLoader: ClassLoader,
) {
    fun load(font: TextGateFont): LoadedFont {
        val bytes = checkNotNull(classLoader.getResourceAsStream(font.resourcePath)) {
            "Font file ${font.resourcePath} is not on the classpath"
        }.use { it.readBytes() }
        val file = File(copyDirectory, "${font.id}.ttf").apply { writeBytes(bytes) }
        val style = if (font.isItalic) FontStyle.Italic else FontStyle.Normal
        return LoadedFont(
            family = FontFamily(Font(file = file, weight = FontWeight(font.weight), style = style)),
            sha256 = Sha256Hex.of(bytes),
        )
    }
}
