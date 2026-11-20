package dev.tonholo.ssrcompose.contracttests.textgate

/**
 * A font file the text gate draws with. [resourcePath] is where the file sits inside its font artifact.
 * Only a weight and a style that have a file of their own are listed.
 */
enum class TextGateFont(
    val id: String,
    val resourcePath: String,
    val weight: Int = REGULAR_WEIGHT,
    val isItalic: Boolean = false,
) {
    RobotoRegular(id = "roboto-regular", resourcePath = "fonts/ttf/Roboto/Roboto-Regular.ttf"),
    OpenSansRegular(id = "open-sans-regular", resourcePath = "fonts/ttf/OpenSans/OpenSans-Regular.ttf"),
}

private const val REGULAR_WEIGHT = 400
