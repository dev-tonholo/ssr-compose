package dev.tonholo.ssrcompose.compiler.testing.scale

/** One way to compile the server module of the scale measurements. */
internal enum class CompileCase {
    /** The called composables are hand-written in the module, with no plugin and no catalog. */
    StandIns,

    /** A catalog that holds only the called composables. */
    CalledComponentsCatalog,

    /** One held catalog of every component. The plugin creates only the bridges the code names. */
    OneFullCatalog,

    /** Ten held catalogs of every component. The plugin creates only the bridges the code names. */
    TenFullCatalogs,

    /** A module with many calls and no bridge, compiled without this plugin. */
    ManyCallsWithoutPlugin,

    /** The same module compiled with `mode=check`. */
    ManyCallsWithCheckMode,
}
