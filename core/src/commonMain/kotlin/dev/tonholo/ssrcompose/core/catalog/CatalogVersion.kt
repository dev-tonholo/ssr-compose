package dev.tonholo.ssrcompose.core.catalog

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * A label the app developer sets for a catalog. It has no order: only equality matters.
 */
@Serializable
@JvmInline
public value class CatalogVersion(public val value: String)
