package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.catalog.index.CatalogIndex
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor

/** The components the code of a module names: those that get a bridge, and why the others cannot. */
internal class NamedComponents(
    val created: List<BridgeComponent>,
    val refusals: Map<String, String>,
)

/** Decodes only the components of an index that the code of the module names. */
internal class NamedBridgeComponents(private val classRule: BridgeClassRule) {
    fun of(index: CatalogIndex, named: Set<String>): NamedComponents {
        val names = index.componentNames.filter { name -> name in named }
        val classRefusals = BridgeLimits.classRefusals(componentNames = names, classRule = classRule)
        val descriptors = names.mapNotNull(index::component)
        val refusals = descriptors.mapNotNull { descriptor ->
            (refusalOf(descriptor = descriptor) ?: classRefusals[descriptor.name])?.let { descriptor.name to it }
        }.toMap()
        return NamedComponents(
            created = descriptors.filterNot { it.name in refusals }.map(::bridgeComponent),
            refusals = refusals,
        )
    }

    private fun refusalOf(descriptor: ComponentDescriptor): String? =
        unsupportedParameters(descriptor = descriptor)
            ?: BridgeLimits.parameterRefusal(
                componentName = descriptor.name,
                parameterCount = descriptor.parameters.size,
            )

    private fun unsupportedParameters(descriptor: ComponentDescriptor): String? =
        descriptor.parameters.filter { it.toBridgeParameter() == null }
            .takeIf { refused -> refused.isNotEmpty() }
            ?.joinToString(separator = " ") { parameter ->
                "The composable '${descriptor.name}' cannot get a bridge: its parameter '${parameter.name}' " +
                    "${parameter.shapeDescription()} is not supported yet."
            }

    private fun bridgeComponent(descriptor: ComponentDescriptor): BridgeComponent =
        BridgeComponent(
            name = descriptor.name,
            parameters = descriptor.parameters.mapNotNull { it.toBridgeParameter() },
        )
}
