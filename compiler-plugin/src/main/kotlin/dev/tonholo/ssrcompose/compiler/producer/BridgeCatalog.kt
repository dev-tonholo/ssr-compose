package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.compiler.producer.throwaway.ThrowawayComponent
import dev.tonholo.ssrcompose.compiler.producer.throwaway.ThrowawayIndex
import dev.tonholo.ssrcompose.compiler.producer.throwaway.ThrowawayIndexReader
import java.io.File
import org.jetbrains.kotlin.name.FqName

internal class BridgeCatalog(
    val key: BridgeKey,
    val bridgePackage: FqName,
    val index: ThrowawayIndex,
) {
    val componentsByName: Map<String, ThrowawayComponent> = index.components.associateBy { it.name }

    companion object {
        fun from(option: CatalogOptionValue): BridgeCatalog =
            BridgeCatalog(
                key = BridgeKey(catalogVersion = option.version),
                bridgePackage = FqName(option.bridgePackage),
                index = ThrowawayIndexReader.read(File(option.indexPath)),
            )
    }
}
