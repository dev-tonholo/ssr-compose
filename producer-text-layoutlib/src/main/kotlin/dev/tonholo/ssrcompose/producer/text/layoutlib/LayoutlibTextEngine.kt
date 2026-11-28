package dev.tonholo.ssrcompose.producer.text.layoutlib

import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.text.BoringLayout
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.math.ceil
import kotlin.math.min

private const val SPACING_MULTIPLIER = 1f
private const val SPACING_ADDED = 0f
private const val INCLUDE_FONT_PADDING = false
private const val USE_FALLBACK_LINE_SPACING = true

/**
 * [TextEngine] on layoutlib's Android `StaticLayout` and `BoringLayout`, with the settings Compose gives them on
 * Android. [runtimeRoot] is the unpacked `layoutlib-runtime` jar of this machine.
 */
internal class LayoutlibTextEngine(runtimeRoot: Path) : TextEngine {
    init {
        LayoutlibBridge.start(runtimeRoot)
    }

    override fun loadFont(bytes: ByteArray): TextFont {
        require(bytes.isNotEmpty()) { "a font cannot be loaded from no bytes" }
        val buffer = ByteBuffer.allocateDirect(bytes.size).put(bytes)
        buffer.rewind()
        val family = FontFamily.Builder(Font.Builder(buffer).build()).build()
        return LayoutlibFont(typeface = Typeface.CustomFallbackBuilder(family).build())
    }

    override fun measure(request: TextRequest): TextMeasurement {
        val paint = paintFor(request)
        val lineHeightPx = request.lineHeight?.composePx(fontSizePx = request.sizePx)
        val text = lineHeightPx?.let { composeTextWithLineHeight(text = request.text, lineHeightPx = it) }
            ?: request.text
        val layout = layOut(text = text, request = request, paint = paint)
        val lastLine = min(a = layout.lineCount, b = request.maxLines) - 1
        val emptyLastLine = lineHeightPx?.let {
            layout.emptyLastLineMetrics(lastLine = lastLine, lineHeightPx = it, paint = paint)
        }
        val lines = ComposeLines(layout = layout, lastLine = lastLine, emptyLastLine = emptyLastLine)
        val shownLines = 0..lastLine
        return TextMeasurement(
            heightPx = lines.bottom(lastLine),
            widthPx = layout.width,
            lineEnds = shownLines.map(layout::shownTextEnd),
            lineRightsPx = shownLines.map(layout::getLineRight),
            lineBottomsPx = shownLines.map(lines::bottom),
            isLastLineEllipsized = layout.getEllipsisCount(lastLine) > 0,
            firstBaselinePx = lines.baseline(0),
            lastBaselinePx = lines.baseline(lastLine),
        )
    }

    private fun paintFor(request: TextRequest): TextPaint {
        val font = requireNotNull(request.font as? LayoutlibFont) { "the font was not loaded by this engine" }
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)
        paint.typeface = font.typeface
        paint.textSize = request.sizePx
        paint.textLocale = request.locale
        return paint
    }

    private fun layOut(text: CharSequence, request: TextRequest, paint: TextPaint): Layout {
        val desiredWidth = ceil(Layout.getDesiredWidth(text, paint)).toInt()
        val width = min(a = desiredWidth, b = request.widthPx)
        val boring = boringMetrics(text = text, paint = paint)
        return if (boring != null && boring.width <= width) {
            boringLayout(text = text, paint = paint, width = width, metrics = boring)
        } else {
            staticLayout(text = text, request = request, paint = paint, width = width)
        }
    }

    private fun boringMetrics(text: CharSequence, paint: TextPaint): BoringLayout.Metrics? = BoringLayout.isBoring(
        text,
        paint,
        TextDirectionHeuristics.FIRSTSTRONG_LTR,
        USE_FALLBACK_LINE_SPACING,
        null,
    )

    private fun boringLayout(text: CharSequence, paint: TextPaint, width: Int, metrics: BoringLayout.Metrics) =
        BoringLayout(
            text,
            paint,
            width,
            Layout.Alignment.ALIGN_NORMAL,
            SPACING_MULTIPLIER,
            SPACING_ADDED,
            metrics,
            INCLUDE_FONT_PADDING,
            null,
            width,
            USE_FALLBACK_LINE_SPACING,
        )

    private fun staticLayout(text: CharSequence, request: TextRequest, paint: TextPaint, width: Int): StaticLayout {
        val builder = composeStaticLayout(text = text, paint = paint, width = width).setMaxLines(request.maxLines)
        if (request.overflow == TextOverflow.Ellipsis) {
            builder.setEllipsize(TextUtils.TruncateAt.END).setEllipsizedWidth(width)
        }
        return builder.build()
    }

    private class LayoutlibFont(val typeface: Typeface) : TextFont
}

internal fun composeStaticLayout(text: CharSequence, paint: TextPaint, width: Int): StaticLayout.Builder =
    StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
        .setLineSpacing(SPACING_ADDED, SPACING_MULTIPLIER)
        .setIncludePad(INCLUDE_FONT_PADDING)
        .setUseLineSpacingFromFallbacks(USE_FALLBACK_LINE_SPACING)
        .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
        .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
        .setJustificationMode(Layout.JUSTIFICATION_MODE_NONE)

private fun Layout.shownTextEnd(line: Int): Int =
    if (getEllipsisCount(line) > 0) getLineStart(line) + getEllipsisStart(line) else composeLineVisibleEnd(line)
