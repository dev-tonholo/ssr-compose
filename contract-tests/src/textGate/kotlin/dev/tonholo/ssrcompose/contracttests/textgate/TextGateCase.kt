package dev.tonholo.ssrcompose.contracttests.textgate

/**
 * One text drawn at one size and font scale. The width, density and line height are the same for every case.
 */
data class TextGateCase(
    val font: TextGateFont,
    val sample: TextGateSample,
    val sizeInSp: Float,
    val fontScale: Float,
) {
    val id: String = "${font.id}/${sample.id}/${sizeInSp.toInt()}sp/x$fontScale"
}
