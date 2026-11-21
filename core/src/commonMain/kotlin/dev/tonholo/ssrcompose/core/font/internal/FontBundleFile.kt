package dev.tonholo.ssrcompose.core.font.internal

import dev.tonholo.ssrcompose.core.font.FontBundleManifest
import dev.tonholo.ssrcompose.core.font.FontBundleVersion
import dev.tonholo.ssrcompose.core.font.FontEntry
import kotlinx.serialization.Serializable

private const val FORMAT = 1

/** `fonts.json` as it is written. */
@Serializable
internal data class FontBundleFile(
    @Serializable(with = StrictIntSerializer::class) val format: Int,
    val version: String,
    val fonts: List<FontEntryFile>,
) {
    companion object {
        fun of(manifest: FontBundleManifest): FontBundleFile = FontBundleFile(
            format = FORMAT,
            version = manifest.version.value,
            fonts = manifest.fonts.map { entry -> FontEntryFile.of(entry = entry) },
        )
    }

    fun toManifest(): FontBundleManifest {
        require(format == FORMAT) { "The font bundle format $format is not known" }
        return FontBundleManifest(
            version = FontBundleVersion(value = version),
            fonts = fonts.map { entry -> entry.toEntry() },
        )
    }
}

@Serializable
internal data class FontEntryFile(
    val family: String,
    @Serializable(with = StrictIntSerializer::class) val weight: Int,
    @Serializable(with = StrictBooleanSerializer::class) val italic: Boolean,
    val file: String,
    val fingerprint: String,
) {
    companion object {
        fun of(entry: FontEntry): FontEntryFile = FontEntryFile(
            family = entry.family,
            weight = entry.weight,
            italic = entry.italic,
            file = entry.file,
            fingerprint = entry.fingerprint,
        )
    }

    fun toEntry(): FontEntry =
        FontEntry(family = family, weight = weight, italic = italic, file = file, fingerprint = fingerprint)
}
