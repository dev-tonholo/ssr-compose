package dev.tonholo.ssrcompose.core.document

import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi
import dev.tonholo.ssrcompose.core.document.internal.DocumentColorSerializer
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * A 32-bit ARGB color, written `"#AARRGGBB"` on the wire.
 */
@InternalSsrComposeApi
@Serializable(with = DocumentColorSerializer::class)
@JvmInline
public value class DocumentColor(public val argb: UInt)
