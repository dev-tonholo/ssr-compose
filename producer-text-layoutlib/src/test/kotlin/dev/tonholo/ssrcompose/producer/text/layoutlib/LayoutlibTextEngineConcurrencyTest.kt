package dev.tonholo.ssrcompose.producer.text.layoutlib

import com.android.layoutlib.bridge.Bridge
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val THREAD_COUNT = 8
private const val ROUND_COUNT = THREAD_COUNT
private const val TIMEOUT_MINUTES = 3L
private const val KEPT_DIFFERENCES = 2
private const val TEXT_FOR_FALLBACK_FONTS = "漢字かな 😀 مرحبا بالعالم สวัสดีชาวโลก नमस्ते"

private val SIZES_PX = (12..71).map { it.toFloat() }
private val WIDTHS_PX = listOf(120, 300, 525)
private val LINE_LIMITS = listOf(
    LineLimit(maxLines = Int.MAX_VALUE, overflow = TextOverflow.Clip),
    LineLimit(maxLines = 2, overflow = TextOverflow.Ellipsis),
)

private data class LineLimit(val maxLines: Int, val overflow: TextOverflow)

private data class Case(
    val text: String,
    val fontFile: String,
    val sizePx: Float,
    val widthPx: Int,
    val lineLimit: LineLimit,
) {
    fun request(fonts: Map<String, TextFont>): TextRequest = TextRequest(
        text = text,
        font = fonts.getValue(fontFile),
        sizePx = sizePx,
        widthPx = widthPx,
        maxLines = lineLimit.maxLines,
        overflow = lineLimit.overflow,
    )
}

private data class Answer(val case: Case, val measurement: TextMeasurement)

class LayoutlibTextEngineConcurrencyTest {
    private val engine = testEngine()
    private val rows = DeviceRecording.tablet().measurements

    private val texts = rows.map { it.text }.distinct() + TEXT_FOR_FALLBACK_FONTS

    private val cases = texts.flatMap { text ->
        rows.map { it.fontFile }.distinct().flatMap { fontFile -> cases(text = text, fontFile = fontFile) }
    }

    private fun cases(text: String, fontFile: String): List<Case> = SIZES_PX.flatMap { size ->
        WIDTHS_PX.flatMap { width ->
            LINE_LIMITS.map { limit ->
                Case(text = text, fontFile = fontFile, sizePx = size, widthPx = width, lineLimit = limit)
            }
        }
    }

    @Test
    fun `given unmeasured fonts and the render lock held, when 8 threads share them, then all equal one thread`() {
        // Arrange
        val orders = List(THREAD_COUNT) { worker -> cases.shuffled(Random(worker)) }

        // Act
        val results = holdingTheRenderLock {
            val fontSets = onEveryThread { engine.loadFonts(rows) }
            fontSets.flatMap { fonts ->
                onEveryThread { worker -> measureAll(order = orders[worker], fonts = fonts) }
            }
        }

        // Assert
        val separateFonts = engine.loadFonts(rows)
        val onOneThread = cases.associateWith { engine.measure(it.request(separateFonts)) }
        val differences = results.asSequence()
            .flatten()
            .filter { it.measurement != onOneThread.getValue(it.case) }
            .take(KEPT_DIFFERENCES)
            .toList()
        val calls = results.sumOf { it.size }
        assertEquals(expected = emptyList(), actual = differences)
        assertEquals(expected = THREAD_COUNT * ROUND_COUNT * cases.size, actual = calls)
    }

    private fun <T> holdingTheRenderLock(block: ExecutorService.() -> T): T {
        val pool = Executors.newFixedThreadPool(THREAD_COUNT)
        Bridge.getLock().lock()
        try {
            return pool.block()
        } finally {
            Bridge.getLock().unlock()
            pool.shutdownNow()
        }
    }

    private fun <T> ExecutorService.onEveryThread(task: (worker: Int) -> T): List<T> {
        val startTogether = CyclicBarrier(THREAD_COUNT)
        val workers = List(THREAD_COUNT) { worker ->
            Callable {
                startTogether.await()
                task(worker)
            }
        }
        val finished = invokeAll(workers, TIMEOUT_MINUTES, TimeUnit.MINUTES)
        val everyThreadFinished = finished.none { it.isCancelled }
        assertTrue(actual = everyThreadFinished, message = "a thread did not finish while the render lock was held")
        return finished.map { it.get() }
    }

    private fun measureAll(order: List<Case>, fonts: Map<String, TextFont>): List<Answer> =
        order.map { case -> Answer(case = case, measurement = engine.measure(case.request(fonts))) }
}
