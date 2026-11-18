package dev.tonholo.ssrcompose.catalog.testing

import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import kotlin.random.Random

private const val PART_SIZE_STEPS = 5
private const val INDEX_DIGITS = 5
private const val PART_DIGITS = 3
private const val PADDING = '0'
private val FEATURES = listOf("Checkout", "Profile", "Search", "Catalog", "Orders", "Settings", "Inbox", "Wallet")
private val ELEMENTS = listOf("Card", "Banner", "Row", "Header", "Sheet", "Tile", "Badge", "Panel", "Dialog")

/** Generates a catalog with the mix real apps have: most components have 2 to 4 parameters, a few 15 or more. */
object SyntheticCatalog {
    fun generate(shape: SyntheticCatalogShape): Catalog {
        val choices = syntheticChoiceLists()
        val sizes = partSizes(components = shape.components, parts = shape.parts)
        val firstIndexes = sizes.runningFold(initial = 0) { index, size -> index + size }
        val parts = sizes.indices.map { partIndex ->
            val partNumber = partIndex.toString().padStart(length = PART_DIGITS, padChar = PADDING)
            CatalogPart(
                name = PartName(value = "feature-$partNumber"),
                choices = choices,
                components = List(size = sizes[partIndex]) { offset ->
                    component(index = firstIndexes[partIndex] + offset, shape = shape, choices = choices)
                },
            )
        }
        return Catalog(version = CatalogVersion(value = shape.version), parts = parts)
    }

    private fun partSizes(components: Int, parts: Int): List<Int> {
        val weights = List(size = parts) { index -> index % PART_SIZE_STEPS + 1 }
        val sizes = weights.map { weight -> components * weight / weights.sum() }
        return listOf(sizes.first() + components - sizes.sum()) + sizes.drop(1)
    }

    private fun component(
        index: Int,
        shape: SyntheticCatalogShape,
        choices: List<ChoiceList>,
    ): ComponentDescriptor {
        val random = Random(seed = shape.seed + index)
        return ComponentDescriptor(
            name = componentName(index = index),
            parameters = List(size = random.nextParameterCount()) { parameterIndex ->
                random.nextParameter(index = parameterIndex, palette = shape.palette, choices = choices)
            },
        )
    }

    private fun componentName(index: Int): String {
        val feature = FEATURES[index % FEATURES.size]
        val element = ELEMENTS[index % ELEMENTS.size]
        return "$feature$element${index.toString().padStart(length = INDEX_DIGITS, padChar = PADDING)}"
    }
}
