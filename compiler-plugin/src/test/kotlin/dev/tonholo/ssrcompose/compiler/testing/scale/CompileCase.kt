package dev.tonholo.ssrcompose.compiler.testing.scale

/**
 * One way to compile the server module of the scale experiment.
 *
 * @property isHeavy true when one compile takes so long that the measurement warms up only once.
 */
internal enum class CompileCase(val isHeavy: Boolean = false) {
    /** The called composables are hand-written in the module, with no plugin and no catalog. */
    StandIns,

    /** A catalog that holds only the called composables. */
    CalledComponentsCatalog,

    /** One held catalog of every component, with a bridge function for each, spread over several classes. */
    OneFullCatalog,

    /** Ten held catalogs of every component, with a bridge function for each, spread over several classes. */
    TenFullCatalogs(isHeavy = true),

    /** One held catalog of every component, with bridge functions only for the names the code uses. */
    OneFullCatalogNamedOnly,

    /** Ten held catalogs of every component, with bridge functions only for the names the code uses. */
    TenFullCatalogsNamedOnly,

    /** A module with many calls and no bridge, compiled without this plugin. */
    ManyCallsWithoutPlugin,

    /** The same module compiled with `mode=check`. */
    ManyCallsWithCheckMode,
}
