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
// androidx/compose/ui/text/android/LayoutHelper.android.kt.

package dev.tonholo.ssrcompose.producer.text.layoutlib

import android.text.Layout

internal fun Layout.composeLineVisibleEnd(line: Int): Int {
    val lineStart = getLineStart(line)
    var visibleEnd = getLineEnd(line)
    while (visibleEnd > lineStart && isLineEndSpace(text[visibleEnd - 1])) {
        visibleEnd--
    }
    return visibleEnd
}

private fun isLineEndSpace(c: Char): Boolean = c == ' ' ||
    c == '\n' ||
    c == '\u1680' ||
    (c in '\u2000'..'\u200A' && c != '\u2007') ||
    c == '\u205F' ||
    c == '\u3000'
