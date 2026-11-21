package dev.tonholo.ssrcompose.core.font

import dev.tonholo.ssrcompose.core.label.requireFolderLabel
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * The version of an app's font bundle, a label the app developer sets. It names the bundle's folder. It has no
 * order: only equal or not equal matters.
 *
 * @throws IllegalArgumentException when the text breaks the folder label rule of contracts/font-bundle.md.
 */
@Serializable
@JvmInline
public value class FontBundleVersion(public val value: String) {
    init {
        requireFolderLabel(label = value, subject = "font bundle version")
    }
}
