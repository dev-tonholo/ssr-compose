package dev.tonholo.ssrcompose.compiler.producer

import org.jetbrains.kotlin.name.FqName

/**
 * The bridges one held catalog creates in a module.
 *
 * @property refusals why each named component that cannot get a bridge is refused, by component name.
 */
internal class BridgeCatalog(
    val key: BridgeKey,
    val bridgePackage: FqName,
    val components: List<BridgeComponent>,
    val refusals: Map<String, String>,
    val classRule: BridgeClassRule,
) {
    val componentsByName: Map<String, BridgeComponent> = components.associateBy { it.name }
}
