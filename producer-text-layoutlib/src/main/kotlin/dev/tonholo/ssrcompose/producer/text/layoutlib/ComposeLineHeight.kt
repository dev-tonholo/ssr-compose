/*
 * Copyright 2020 The Android Open Source Project
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
// androidx/compose/ui/text/platform/extensions/SpannableExtensions.android.kt: only the line height in sp with the
// default LineHeightStyle is kept, and the caller passes the font px converted back to sp.

package dev.tonholo.ssrcompose.producer.text.layoutlib

import android.text.SpannableString
import android.text.Spanned

private const val LARGEST_LINEAR_FONT_SCALE = 1.05

/**
 * The line height Compose lays out for a text of [fontSizePx]. With non-linear font scaling it keeps the ratio of
 * line height to font size in sp, since the density would shrink a line height much larger than the font.
 */
internal fun TextLineHeight.composePx(fontSizePx: Float): Float =
    if (fontScale > LARGEST_LINEAR_FONT_SCALE) sp / sizePxAsSp * fontSizePx else px

internal fun composeTextWithLineHeight(text: String, lineHeightPx: Float): CharSequence {
    val endIndex = if (text.isEmpty() || text.last() == '\n') text.length + 1 else text.length
    val spannable = SpannableString(text)
    spannable.setSpan(
        LineHeightStyleSpan(lineHeightPx = lineHeightPx, endIndex = endIndex),
        0,
        text.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    return spannable
}
