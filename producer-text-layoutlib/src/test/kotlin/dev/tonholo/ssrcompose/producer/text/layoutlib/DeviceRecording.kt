package dev.tonholo.ssrcompose.producer.text.layoutlib

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

private const val TABLET_RECORDING = "text-recordings/api36-tablet-rows-used-by-tests.json"

private val json = Json { ignoreUnknownKeys = true }

/**
 * What a physical API 36 tablet measured for each text. The resource holds only the rows these tests use, copied
 * unchanged. The full recording is not in the repo: it is the git-ignored
 * `.crew/reports-layoutlib/round2/device-files/api36-tablet-physical.json` of the owner's main checkout.
 * L2b-2 brings the full replay and decides how to store it.
 */
@Serializable
internal data class DeviceRecording(val measurements: List<DeviceRow>) {
    fun row(caseId: String): DeviceRow = measurements.single { it.caseId == caseId }

    companion object {
        fun tablet(): DeviceRecording = json.decodeFromString(classpathBytes(TABLET_RECORDING).decodeToString())
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
    val lineVisibleEnds: List<Int>,
    val lineBottomsInPx: List<Float>,
    val firstBaselineInPx: Float,
    val lastBaselineInPx: Float,
) {
    fun request(font: TextFont): TextRequest =
        TextRequest(text = text, font = font, sizePx = fontSizeInPx, widthPx = widthInPx)

    fun measurement(): TextMeasurement = TextMeasurement(
        heightPx = heightInPx,
        lineEnds = lineVisibleEnds,
        firstBaselinePx = firstBaselineInPx.roundToInt(),
        lastBaselinePx = lastBaselineInPx.roundToInt(),
    )
}

internal fun classpathBytes(path: String): ByteArray =
    requireNotNull(DeviceRecording::class.java.classLoader.getResourceAsStream(path)) { "no resource $path" }
        .use { it.readBytes() }
