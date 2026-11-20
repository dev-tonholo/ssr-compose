package dev.tonholo.ssrcompose.core.handshake

import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.font.FontBundleVersion
import kotlinx.serialization.Serializable

/**
 * What the consumer says about itself first. It never carries a catalog or a font file.
 * [fontVersion] is null when the app declares no font.
 */
@Serializable
public data class Introduction(
    val contractVersion: ContractVersion,
    val catalogVersion: CatalogVersion,
    val fontVersion: FontBundleVersion? = null,
    val app: AppIdentity,
    val device: DeviceContext,
)
