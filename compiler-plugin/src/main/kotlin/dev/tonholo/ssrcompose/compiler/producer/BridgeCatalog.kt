package dev.tonholo.ssrcompose.compiler.producer

import org.jetbrains.kotlin.name.FqName

internal class BridgeCatalog(
    val key: BridgeKey,
    val bridgePackage: FqName,
    val components: List<BridgeComponent>,
) {
    val componentsByName: Map<String, BridgeComponent> = components.associateBy { it.name }
}
