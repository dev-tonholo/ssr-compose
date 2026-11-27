/*
 * Copyright (C) 2016 The Android Open Source Project
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
// Modified for ssr-compose from Paparazzi 2.0.0-alpha05, app/cash/paparazzi/internal/Renderer.kt.

package dev.tonholo.ssrcompose.producer.text.layoutlib

import com.android.layoutlib.bridge.Bridge
import java.nio.file.Path
import java.util.Locale

private const val BUILD_PROPERTIES = "build.prop"
private const val DATA_FOLDER = "data"
private const val FONTS_FOLDER = "fonts"
private const val HYPHENATION_FOLDER = "hyphen-data"
private const val ICU_DATA = "icu/icudt76l.dat"
private const val KEYBOARD = "keyboards/Generic.kcm"
private const val MAC_LIBRARIES = "mac-arm/lib64"
private const val LINUX_LIBRARIES = "linux/lib64"

/** Starts layoutlib's native Android classes, once for the whole process, with no render session. */
internal object LayoutlibBridge {
    private var startedFrom: Path? = null

    /**
     * @throws IllegalArgumentException when layoutlib already started from another folder.
     * @throws IllegalStateException when layoutlib cannot start from [runtimeRoot].
     */
    @Synchronized
    fun start(runtimeRoot: Path) {
        val root = runtimeRoot.toAbsolutePath().normalize()
        val earlierRoot = startedFrom
        if (earlierRoot == null) {
            initialize(root)
            startedFrom = root
        } else {
            require(earlierRoot == root) { "layoutlib already started from $earlierRoot, not $root" }
        }
    }

    private fun initialize(root: Path) {
        val data = root.resolve(DATA_FOLDER)
        val log = StartupLog()
        val isStarted = Bridge().init(
            buildProperties(root.resolve(BUILD_PROPERTIES)),
            data.resolve(FONTS_FOLDER).toFile(),
            data.resolve(nativeLibraryFolder()).toString(),
            data.resolve(ICU_DATA).toString(),
            data.resolve(HYPHENATION_FOLDER).toString(),
            arrayOf(data.resolve(KEYBOARD).toString()),
            emptyMap(),
            log,
        )
        if (!isStarted) {
            val reason = log.firstMessage ?: "an earlier start failed in this process, and layoutlib does not retry"
            throw IllegalStateException("layoutlib did not start from $root: $reason", log.firstCause)
        }
    }

    private fun nativeLibraryFolder(): String {
        val isMac = System.getProperty("os.name").lowercase(Locale.US).startsWith("mac")
        return if (isMac) MAC_LIBRARIES else LINUX_LIBRARIES
    }
}
