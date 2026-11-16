package dev.tonholo.ssrcompose.core.handshake

import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import kotlinx.serialization.Serializable

/**
 * What the consumer says about itself first. It never carries a catalog.
 */
@Serializable
public data class Introduction(
    val contractVersion: ContractVersion,
    val catalogVersion: CatalogVersion,
    val app: AppIdentity,
    val device: DeviceContext,
)
