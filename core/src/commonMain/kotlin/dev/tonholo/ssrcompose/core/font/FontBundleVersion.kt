package dev.tonholo.ssrcompose.core.font

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * The version of an app's font bundle, a label the app developer sets. It names the bundle's folder. It has no
 * order: only equal or not equal matters.
 */
@Serializable
@JvmInline
public value class FontBundleVersion(public val value: String)
