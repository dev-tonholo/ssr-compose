package dev.tonholo.ssrcompose.gradle

import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * One held catalog of the server project, inside `producer { catalog(version) { } }`.
 *
 * @property version the catalog version this block declares.
 */
abstract class HeldCatalogSettings @Inject constructor(val version: String) {
    /** The address of the catalog's manifest: a web address or a file path. */
    abstract val location: Property<String>

    /** The package the compiler creates this catalog's bridge in. */
    abstract val bridgePackage: Property<String>
}
