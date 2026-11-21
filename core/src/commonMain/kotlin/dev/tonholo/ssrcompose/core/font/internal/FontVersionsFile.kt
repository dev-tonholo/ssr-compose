package dev.tonholo.ssrcompose.core.font.internal

import kotlinx.serialization.Serializable

/** The index of font versions as it is written. */
@Serializable
internal data class FontVersionsFile(val versions: List<String>)
