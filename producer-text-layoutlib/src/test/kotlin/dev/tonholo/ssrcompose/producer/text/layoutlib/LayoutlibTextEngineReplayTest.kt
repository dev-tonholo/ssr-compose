package dev.tonholo.ssrcompose.producer.text.layoutlib

import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals

private const val ROWS_PER_RECORDING = 360
private const val DIFFERENCES_SHOWN = 3

/**
 * How far a line's right edge may be from the device's in [API_30_TO_32_RECORDINGS], and nowhere else. On Roboto rows
 * those recordings end some lines up to 1/64 px short of the API 36 emulator of the same density, which the engine
 * equals, so no engine can equal both. The owner set API 30 as the line for exact text knowing this. The limit comes
 * from `specs/001-consumer-catalog-bridge/research.md`, R20, "Tests that replace T123".
 */
private const val API_30_TO_32_LINE_RIGHT_TOLERANCE_PX = 0.02f

/**
 * The recordings hold no font px converted back to sp, so the replay passes `sizeInSp` as
 * [TextLineHeight.sizePxAsSp]. By Compose 1.12.1's font scale curve the two are less than 0.000002 sp apart on these
 * rows. The replay checks that no row's rounded line height changes within twice that.
 */
private const val SP_ROUND_TRIP_ERROR = 0.000004f

private val API_30_TO_32_RECORDINGS = listOf(
    "api30-emulator-step2",
    "api31-emulator-step2",
    "api32-emulator-step2",
)

private val LISTED_RECORDINGS = API_30_TO_32_RECORDINGS + listOf(
    "api33-motorola-physical",
    "api35-step2-emulator",
    "api36-step2",
    "api36-tablet-physical",
    "api37-emulator",
    "api37-pixel7pro",
)

private data class ReplayedRow(
    val row: DeviceRow,
    val measured: TextMeasurement,
    val lineRightTolerancePx: Float,
) {
    val isSameAsDevice: Boolean
        get() {
            val device = row.measurement()
            return measured.lineCount == row.lineCount &&
                measured.copy(lineRightsPx = device.lineRightsPx) == device &&
                measured.lineRightsPx.isWithin(tolerancePx = lineRightTolerancePx, others = device.lineRightsPx)
        }

    private fun List<Float>.isWithin(tolerancePx: Float, others: List<Float>): Boolean =
        size == others.size && zip(others).all { (mine, other) -> abs(mine - other) <= tolerancePx }
}

private data class Outcome(val rows: Int, val differingRows: Int, val firstDifferences: List<String>)

private data class Control(val rows: Int, val matchingRows: Int)

class LayoutlibTextEngineReplayTest {
    private val engine = testEngine()

    @Test
    fun `given every text recorded from API 30, when replayed, then each row gives the device's numbers`() {
        // Arrange
        val recordings = DeviceRecording.namesInTheFolder().associateWith(DeviceRecording::named)

        // Act
        val replayed = recordings.mapValues { (name, recording) -> replay(name = name, rows = recording.measurements) }

        // Assert
        val everyRowSame = Outcome(rows = ROWS_PER_RECORDING, differingRows = 0, firstDifferences = emptyList())
        assertEquals(
            expected = LISTED_RECORDINGS.associateWith { everyRowSame },
            actual = replayed.mapValues { (_, rows) -> outcome(rows) },
        )
    }

    @Test
    fun `given every text drawn 1 dp larger, when replayed, then no row of any recording matches`() {
        // Arrange
        val recordings = DeviceRecording.namesInTheFolder().associateWith(DeviceRecording::named)

        // Act
        val replayed = recordings.mapValues { (name, recording) ->
            val oneDpInPx = recording.density
            replay(name = name, rows = recording.measurements, extraSizePx = oneDpInPx)
        }

        // Assert
        val noRowSame = Control(rows = ROWS_PER_RECORDING, matchingRows = 0)
        assertEquals(
            expected = LISTED_RECORDINGS.associateWith { noRowSame },
            actual = replayed.mapValues { (_, rows) ->
                Control(rows = rows.size, matchingRows = rows.count { it.isSameAsDevice })
            },
        )
    }

    private fun replay(name: String, rows: List<DeviceRow>, extraSizePx: Float = 0f): List<ReplayedRow> {
        checkSameFontBytes(rows)
        val fonts = engine.loadFonts(rows)
        val tolerancePx = if (name in API_30_TO_32_RECORDINGS) API_30_TO_32_LINE_RIGHT_TOLERANCE_PX else 0f
        return rows.map { row ->
            val request = row.request(fonts.getValue(row.fontFile))
            checkRecordedSizeRoundsAsTheConvertedOne(caseId = row.caseId, request = request)
            val drawn = request.copy(sizePx = request.sizePx + extraSizePx)
            ReplayedRow(row = row, measured = engine.measure(drawn), lineRightTolerancePx = tolerancePx)
        }
    }

    private fun outcome(rows: List<ReplayedRow>): Outcome {
        val differing = rows.filterNot { it.isSameAsDevice }
        val shown = differing.take(DIFFERENCES_SHOWN).map {
            "${it.row.caseId}: device ${it.row.measurement()}, engine ${it.measured}"
        }
        return Outcome(rows = rows.size, differingRows = differing.size, firstDifferences = shown)
    }

    private fun checkRecordedSizeRoundsAsTheConvertedOne(caseId: String, request: TextRequest) {
        val lineHeight = request.lineHeight ?: return
        val roundedHeights = listOf(-SP_ROUND_TRIP_ERROR, 0f, SP_ROUND_TRIP_ERROR).map { error ->
            val nearby = lineHeight.copy(sizePxAsSp = lineHeight.sizePxAsSp + error)
            ceil(nearby.composePx(fontSizePx = request.sizePx))
        }
        check(roundedHeights.distinct().size == 1) {
            "$caseId: the recorded size in sp may not round the line height as the converted font px does"
        }
    }

    private fun checkSameFontBytes(rows: List<DeviceRow>) {
        rows.map { it.fontFile to it.fontSha256 }.distinct().forEach { (fontFile, deviceSha256) ->
            val digest = MessageDigest.getInstance("SHA-256").digest(classpathBytes(fontFile))
            check(digest.toHexString() == deviceSha256) { "$fontFile is not the font the device measured with" }
        }
    }
}
