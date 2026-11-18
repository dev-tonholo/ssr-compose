package dev.tonholo.ssrcompose.catalog.testing

/**
 * What [SyntheticCatalog] generates. The same shape always gives the same catalog.
 *
 * @property components how many components the catalog holds, over all its parts.
 * @property parts how many parts the components are spread over.
 * @property seed the start of the random choices.
 * @property palette the parameters the components may have.
 * @property version the catalog version.
 */
data class SyntheticCatalogShape(
    val components: Int,
    val parts: Int,
    val seed: Long,
    val palette: ParameterPalette,
    val version: String = "synthetic",
)

/** The parameters a generated component may have. */
enum class ParameterPalette {
    /** Every kind a catalog can describe: values, choices, lists, callbacks and slots. */
    Full,

    /** Only what the bridge experiment of T025 can create: a text, a callback of one text and a plain slot. */
    BridgeExperiment,
}
