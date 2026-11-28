/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Modified for ssr-compose from Compose 1.12.1 ui-text-android,
// androidx/compose/ui/text/android/TextLayout.android.kt: only the bottom and baseline of each line are kept, with
// the rule for an empty last line that has a line height.

package dev.tonholo.ssrcompose.producer.text.layoutlib

import android.graphics.Paint.FontMetricsInt
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint

private const val ZERO_WIDTH_SPACE = "\u200B"

/**
 * Where Compose puts the lines of [layout] up to [lastLine]. [emptyLastLine] replaces the metrics of that line.
 */
internal class ComposeLines(
    private val layout: Layout,
    private val lastLine: Int,
    private val emptyLastLine: FontMetricsInt?,
) {
    fun bottom(line: Int): Int {
        val metrics = replacedMetrics(line) ?: return layout.getLineBottom(line)
        return layout.getLineTop(line) + metrics.bottom
    }

    fun baseline(line: Int): Int {
        val metrics = replacedMetrics(line) ?: return layout.getLineBaseline(line)
        return layout.getLineTop(line) - metrics.ascent
    }

    private fun replacedMetrics(line: Int): FontMetricsInt? = emptyLastLine.takeIf { line == lastLine }
}

/**
 * The metrics of [lastLine] when it holds no character, or null when it holds one. Android applies no line height
 * span to such a line, so this measures it as if it held one that draws nothing.
 */
internal fun Layout.emptyLastLineMetrics(lastLine: Int, lineHeightPx: Float, paint: TextPaint): FontMetricsInt? {
    if (getLineStart(lastLine) != getLineEnd(lastLine)) return null
    val standIn = SpannableString(ZERO_WIDTH_SPACE)
    standIn.setSpan(
        LineHeightStyleSpan(lineHeightPx = lineHeightPx, endIndex = standIn.length, trimFirstLineTop = lastLine == 0),
        0,
        standIn.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    val measured = composeStaticLayout(text = standIn, paint = paint, width = Int.MAX_VALUE).build()
    return FontMetricsInt().also {
        it.ascent = measured.getLineAscent(0)
        it.descent = measured.getLineDescent(0)
        it.top = measured.getLineTop(0)
        it.bottom = measured.getLineBottom(0)
    }
}
