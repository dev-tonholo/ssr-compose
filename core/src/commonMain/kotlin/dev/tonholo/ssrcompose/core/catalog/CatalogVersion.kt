package dev.tonholo.ssrcompose.core.catalog

import dev.tonholo.ssrcompose.core.label.requireFolderLabel
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

private const val SUBJECT = "catalog version"

/**
 * A label the app developer sets. It has no order: only equal or not equal matters.
 *
 * It names the folder `ssr/catalogs/<version>/`, so it holds 1 to 64 characters of lowercase ASCII letters,
 * digits, dots, hyphens and underscores, neither starts nor ends with a dot, and is never a Windows device name
 * (`con`, `prn`, `aux`, `nul`, `com1` to `com9`, `lpt1` to `lpt9`), alone or before a dot.
 *
 * @throws IllegalArgumentException when the text breaks that rule.
 */
@Serializable
@JvmInline
public value class CatalogVersion(public val value: String) {
    init {
        requireFolderLabel(label = value, subject = SUBJECT)
    }
}
