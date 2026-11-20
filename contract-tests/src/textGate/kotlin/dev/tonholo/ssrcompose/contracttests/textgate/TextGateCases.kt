package dev.tonholo.ssrcompose.contracttests.textgate

/**
 * The one list of cases for the text gate. The device half draws them and the producer half measures them.
 */
object TextGateCases {
    const val WIDTH_IN_DP = 200f

    private val sizesInSp = listOf(12f, 16f, 24f)
    private val fontScales = listOf(1f, 1.3f)

    val all: List<TextGateCase> = TextGateFont.entries.flatMap { font ->
        TextGateSample.entries.flatMap { sample ->
            sizesInSp.flatMap { size ->
                fontScales.map { scale ->
                    TextGateCase(font = font, sample = sample, sizeInSp = size, fontScale = scale)
                }
            }
        }
    }
}
