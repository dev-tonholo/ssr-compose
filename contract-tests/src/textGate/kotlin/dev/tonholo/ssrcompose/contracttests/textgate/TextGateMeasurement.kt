package dev.tonholo.ssrcompose.contracttests.textgate

import kotlinx.serialization.Serializable

/**
 * The numbers of one case with every input that made them, so a producer can replay the case from the file alone.
 * [fontSizeInPx] is the pixel size the device gave the size in sp at this font scale.
 */
@Serializable
data class TextGateMeasurement(
    val caseId: String,
    val text: String,
    val fontFile: String,
    val fontSha256: String,
    val weight: Int,
    val isItalic: Boolean,
    val sizeInSp: Float,
    val fontScale: Float,
    val widthInDp: Float,
    val widthInPx: Int,
    val density: Float,
    val fontSizeInPx: Float,
    val lineCount: Int,
    val heightInPx: Int,
    val heightInDp: Float,
) {
    companion object {
        fun of(
            case: TextGateCase,
            fontSha256: String,
            density: Float,
            layout: DeviceTextLayout,
        ): TextGateMeasurement = TextGateMeasurement(
            caseId = case.id,
            text = case.sample.text,
            fontFile = case.font.resourcePath,
            fontSha256 = fontSha256,
            weight = case.font.weight,
            isItalic = case.font.isItalic,
            sizeInSp = case.sizeInSp,
            fontScale = case.fontScale,
            widthInDp = TextGateCases.WIDTH_IN_DP,
            widthInPx = layout.widthInPx,
            density = density,
            fontSizeInPx = layout.fontSizeInPx,
            lineCount = layout.lineCount,
            heightInPx = layout.heightInPx,
            heightInDp = layout.heightInPx / density,
        )
    }
}
