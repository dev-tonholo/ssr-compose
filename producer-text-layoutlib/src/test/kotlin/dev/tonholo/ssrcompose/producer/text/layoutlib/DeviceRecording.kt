package dev.tonholo.ssrcompose.producer.text.layoutlib

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.math.roundToInt

private val RECORDING_LOCALE = Locale.US
private const val TABLET_RECORDING = "api36-tablet-physical"

private val json = Json { ignoreUnknownKeys = true }

/** What one device measured for each text. `text-recordings/README.md` says where each file came from. */
@Serializable
internal data class DeviceRecording(val measurements: List<DeviceRow>) {
    fun row(caseId: String): DeviceRow = measurements.single { it.caseId == caseId }

    companion object {
        fun named(name: String): DeviceRecording =
            json.decodeFromString(classpathBytes("text-recordings/$name.json").decodeToString())

        fun tablet(): DeviceRecording = named(TABLET_RECORDING)
    }
}

@Serializable
internal data class DeviceRow(
    val caseId: String,
    val text: String,
    val fontFile: String,
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
    val lineHeightInPx: Float? = null,
) {
    fun request(font: TextFont): TextRequest = TextRequest(
        text = text,
        font = font,
        sizePx = fontSizeInPx,
        widthPx = widthInPx,
        locale = RECORDING_LOCALE,
        lineHeightPx = lineHeightInPx,
        maxLines = maxLines ?: Int.MAX_VALUE,
        overflow = if (maxLines == null) TextOverflow.Clip else TextOverflow.Ellipsis,
    )

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
