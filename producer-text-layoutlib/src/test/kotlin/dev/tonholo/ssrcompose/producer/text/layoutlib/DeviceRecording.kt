package dev.tonholo.ssrcompose.producer.text.layoutlib

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.math.roundToInt

private val RECORDING_LOCALE = Locale.US
private const val TABLET_RECORDING = "api36-tablet-physical"
private const val RECORDINGS_FOLDER = "text-recordings"

private val json = Json { ignoreUnknownKeys = true }

/** What one device measured for each text. `text-recordings/README.md` says where each file came from. */
@Serializable
internal data class DeviceRecording(val density: Float, val measurements: List<DeviceRow>) {
    fun row(caseId: String): DeviceRow = measurements.single { it.caseId == caseId }

    companion object {
        fun named(name: String): DeviceRecording =
            json.decodeFromString(classpathBytes("$RECORDINGS_FOLDER/$name.json").decodeToString())

        fun namesInTheFolder(): List<String> {
            val folder = requireNotNull(DeviceRecording::class.java.classLoader.getResource(RECORDINGS_FOLDER))
            return Path.of(folder.toURI()).listDirectoryEntries("*.json").map { it.nameWithoutExtension }
        }

        fun tablet(): DeviceRecording = named(TABLET_RECORDING)
    }
}

@Serializable
internal data class DeviceRow(
    val caseId: String,
    val text: String,
    val fontFile: String,
    val fontSha256: String,
    val sizeInSp: Float,
    val fontScale: Float,
    val fontSizeInPx: Float,
    val widthInPx: Int,
    val lineCount: Int,
    val heightInPx: Int,
    val textWidthInPx: Int,
    val lineVisibleEnds: List<Int>,
    val lineRightsInPx: List<Float>,
    val lineBottomsInPx: List<Float>,
    val isLastLineEllipsized: Boolean,
    val firstBaselineInPx: Float,
    val lastBaselineInPx: Float,
    val maxLines: Int? = null,
    val lineHeightInSp: Float? = null,
    val lineHeightInPx: Float? = null,
) {
    fun request(font: TextFont): TextRequest = TextRequest(
        text = text,
        font = font,
        sizePx = fontSizeInPx,
        widthPx = widthInPx,
        locale = RECORDING_LOCALE,
        lineHeight = lineHeight(),
        maxLines = maxLines ?: Int.MAX_VALUE,
        overflow = if (maxLines == null) TextOverflow.Clip else TextOverflow.Ellipsis,
    )

    private fun lineHeight(): TextLineHeight? = lineHeightInSp?.let { sp ->
        TextLineHeight(sp = sp, px = requireNotNull(lineHeightInPx), sizePxAsSp = sizeInSp, fontScale = fontScale)
    }

    fun measurement(): TextMeasurement = TextMeasurement(
        heightPx = heightInPx,
        widthPx = textWidthInPx,
        lineEnds = lineVisibleEnds,
        lineRightsPx = lineRightsInPx,
        lineBottomsPx = lineBottomsInPx.map { it.roundToInt() },
        isLastLineEllipsized = isLastLineEllipsized,
        firstBaselinePx = firstBaselineInPx.roundToInt(),
        lastBaselinePx = lastBaselineInPx.roundToInt(),
    )
}

internal fun classpathBytes(path: String): ByteArray =
    requireNotNull(DeviceRecording::class.java.classLoader.getResourceAsStream(path)) { "no resource $path" }
        .use { it.readBytes() }
