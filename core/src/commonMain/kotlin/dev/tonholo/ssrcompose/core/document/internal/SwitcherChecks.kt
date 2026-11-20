package dev.tonholo.ssrcompose.core.document.internal

import dev.tonholo.ssrcompose.core.document.DocumentNode

private val VARIANT_COUNT = 2..16

internal fun requireVariants(switcher: DocumentNode.Switcher) {
    require(switcher.variants.size in VARIANT_COUNT) {
        "A switcher has $VARIANT_COUNT variants, was ${switcher.variants.size}"
    }
    require(switcher.selected in switcher.variants.indices) {
        "The selected variant ${switcher.selected} is not a variant of the switcher"
    }
}

internal fun requireSwitcherSizes(switcher: DocumentNode.Switcher) {
    requireSize(value = switcher.largestWidth, what = "largest width")
    requireSize(value = switcher.largestHeight, what = "largest height")
    val widest = switcher.variants.maxOf { variant -> variant.maxOfOrNull { node -> node.x + node.width } ?: 0f }
    val tallest = switcher.variants.maxOf { variant -> variant.maxOfOrNull { node -> node.y + node.height } ?: 0f }
    require(switcher.largestWidth >= maxOf(switcher.width, widest)) {
        "The largest width of a switcher is smaller than one of its variants"
    }
    require(switcher.largestHeight >= maxOf(switcher.height, tallest)) {
        "The largest height of a switcher is smaller than one of its variants"
    }
}
