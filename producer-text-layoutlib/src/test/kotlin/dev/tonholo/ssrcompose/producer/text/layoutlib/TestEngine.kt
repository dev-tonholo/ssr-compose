package dev.tonholo.ssrcompose.producer.text.layoutlib

import java.nio.file.Path

private const val RUNTIME_ROOT_PROPERTY = "ssrcompose.layoutlib.runtime"

internal fun testEngine(): LayoutlibTextEngine {
    val runtimeRoot = requireNotNull(System.getProperty(RUNTIME_ROOT_PROPERTY)) { "no $RUNTIME_ROOT_PROPERTY" }
    return LayoutlibTextEngine(runtimeRoot = Path.of(runtimeRoot))
}

internal fun TextEngine.loadFonts(rows: List<DeviceRow>): Map<String, TextFont> =
    rows.map { it.fontFile }.distinct().associateWith { loadFont(classpathBytes(it)) }
