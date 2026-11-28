package dev.tonholo.ssrcompose.producer.internal.font

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import dev.tonholo.ssrcompose.core.fingerprint.Fingerprint

private const val ROBOTO_RESOURCE_DIRECTORY = "fonts/ttf/Roboto"

/**
 * The bundled Roboto files of the library's default family: weights 400, 500 and 700, and 400 italic.
 */
internal enum class RobotoFace(
    private val fileName: String,
    val weight: FontWeight,
    val style: FontStyle,
) {
    Regular(fileName = "Roboto-Regular.ttf", weight = FontWeight.Normal, style = FontStyle.Normal),
    Medium(fileName = "Roboto-Medium.ttf", weight = FontWeight.Medium, style = FontStyle.Normal),
    Bold(fileName = "Roboto-Bold.ttf", weight = FontWeight.Bold, style = FontStyle.Normal),
    Italic(fileName = "Roboto-Italic.ttf", weight = FontWeight.Normal, style = FontStyle.Italic),
    ;

    fun readBytes(): ByteArray {
        val resource = "$ROBOTO_RESOURCE_DIRECTORY/$fileName"
        val stream = checkNotNull(RobotoFace::class.java.classLoader.getResourceAsStream(resource)) {
            "The producer is missing its bundled font $resource"
        }
        return stream.use { it.readBytes() }
    }

    fun toFont(): Font {
        val data = readBytes()
        val fingerprint = Fingerprint.of(bytes = data)
        return fingerprintedFont(data = data, fingerprint = fingerprint, weight = weight, style = style)
    }
}

private val bundledRoboto: FontFamily by lazy { FontFamily(RobotoFace.entries.map { it.toFont() }) }

/** The library's default family, read once for the process. */
internal fun bundledRobotoFamily(): FontFamily = bundledRoboto
