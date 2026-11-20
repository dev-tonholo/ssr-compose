package dev.tonholo.ssrcompose.core.font.internal

import dev.tonholo.ssrcompose.core.font.FontEntry
import dev.tonholo.ssrcompose.core.identifier.isKotlinIdentifier
import dev.tonholo.ssrcompose.core.label.isWindowsDeviceName

private const val DEFAULT_FAMILY = "default"
private const val FINGERPRINT_LENGTH = 64
private val FONT_EXTENSIONS = listOf("ttf", "otf")

internal fun requireFamily(family: String) {
    require(isKotlinIdentifier(text = family) && family != DEFAULT_FAMILY && !isWindowsDeviceName(name = family)) {
        "A font family must be a plain Kotlin identifier other than $DEFAULT_FAMILY and not a Windows device name"
    }
}

internal fun requireFileName(entry: FontEntry) {
    val style = if (entry.italic) "italic" else "normal"
    val expected = FONT_EXTENSIONS.map { extension -> "${entry.family}/${entry.weight}-$style.$extension" }
    require(entry.file in expected) { "The file of a font must be one of $expected" }
}

internal fun requireFingerprint(fingerprint: String) {
    require(fingerprint.length == FINGERPRINT_LENGTH && fingerprint.all { it in '0'..'9' || it in 'a'..'f' }) {
        "A font fingerprint must be $FINGERPRINT_LENGTH lowercase hexadecimal digits"
    }
}

private val FONT_ORDER = compareBy<FontEntry>({ it.family }, { it.weight }, { it.italic })

internal fun requireFamiliesDistinctIgnoringCase(fonts: List<FontEntry>) {
    val sharingFolder = fonts.map { font -> font.family }
        .distinct()
        .groupBy { family -> family.uppercase().lowercase() }
        .values
        .firstOrNull { spellings -> spellings.size > 1 }
    require(sharingFolder == null) { "The font families $sharingFolder differ only by case" }
}

internal fun requireSortedAndDistinct(fonts: List<FontEntry>) {
    fonts.zipWithNext().forEach { (previous, next) ->
        require(FONT_ORDER.compare(previous, next) < 0) {
            "Fonts must be sorted by family, weight and style, each listed once, near ${next.family} ${next.weight}"
        }
    }
}
