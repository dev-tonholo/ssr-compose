/*
 * Copyright 2019 The Android Open Source Project
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
// androidx/compose/ui/text/android/style/LineHeightStyleSpan.android.kt: only the default LineHeightStyle
// (proportional alignment, trim both, fixed mode) is kept.

package dev.tonholo.ssrcompose.producer.text.layoutlib

import android.graphics.Paint.FontMetricsInt
import android.text.style.LineHeightSpan
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Makes each line of its paragraph [lineHeightPx] tall, without the extra space above the first line and below the
 * last. [endIndex] is where the last line ends. For a text that ends in an empty line it is one past the text, so
 * that the line before the empty one is not trimmed as the last.
 */
internal class LineHeightStyleSpan(
    private val lineHeightPx: Float,
    private val endIndex: Int,
    private val trimFirstLineTop: Boolean = true,
) : LineHeightSpan {
    private var firstAscent: Int = Int.MIN_VALUE
    private var ascent: Int = Int.MIN_VALUE
    private var descent: Int = Int.MIN_VALUE
    private var lastDescent: Int = Int.MIN_VALUE

    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanStartVertical: Int,
        lineHeight: Int,
        fontMetricsInt: FontMetricsInt,
    ) {
        if (fontMetricsInt.lineHeight() <= 0) return

        val isFirstLine = start == 0
        val isLastLine = end == endIndex
        if (isFirstLine && isLastLine && trimFirstLineTop) return

        if (firstAscent == Int.MIN_VALUE) {
            calculateTargetMetrics(fontMetricsInt)
        }

        fontMetricsInt.ascent = if (isFirstLine) firstAscent else ascent
        fontMetricsInt.descent = if (isLastLine) lastDescent else descent
    }

    private fun calculateTargetMetrics(fontMetricsInt: FontMetricsInt) {
        val ceiledLineHeight = ceil(lineHeightPx).toInt()
        val diff = ceiledLineHeight - fontMetricsInt.lineHeight()
        val ascentRatio = abs(fontMetricsInt.ascent.toFloat()) / fontMetricsInt.lineHeight()
        val descentDiff =
            if (diff <= 0) {
                ceil(diff * ascentRatio).toInt()
            } else {
                ceil(diff * (1f - ascentRatio)).toInt()
            }

        descent = fontMetricsInt.descent + descentDiff
        ascent = descent - ceiledLineHeight
        firstAscent = if (trimFirstLineTop) fontMetricsInt.ascent else ascent
        lastDescent = fontMetricsInt.descent
    }

    private fun FontMetricsInt.lineHeight(): Int = descent - ascent
}
