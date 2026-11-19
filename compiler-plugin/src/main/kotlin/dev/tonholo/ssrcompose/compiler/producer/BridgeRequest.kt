package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import java.io.File
import java.nio.file.Path

/**
 * What a module needs to create its bridges.
 *
 * @property sourceRoots the folders and files whose code decides which bridges are created.
 * @property excludedSources the files under [sourceRoots] the compilation does not receive.
 * @property moduleName the name of the module, which its bridge classes carry.
 */
internal class BridgeRequest(
    val catalogs: List<CatalogOptionValue>,
    val sourceRoots: List<File>,
    val excludedSources: Set<Path>,
    val moduleName: String,
)
