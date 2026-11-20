package dev.tonholo.ssrcompose.contracttests.textgate

/** What the device reported for one drawn text: its line count, and its size in pixels. */
data class DeviceTextLayout(
    val lineCount: Int,
    val widthInPx: Int,
    val heightInPx: Int,
    val fontSizeInPx: Float,
)
