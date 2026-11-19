package dev.tonholo.ssrcompose.gradle

import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * One held catalog of the server project, inside `producer { catalog(version) { } }`.
 *
 * @property version the catalog version this block declares.
 */
public abstract class HeldCatalogSettings @Inject constructor(public val version: String) {
    /** The address of the catalog's manifest: a web address or a file path. */
    public abstract val location: Property<String>

    /** The package the compiler creates this catalog's bridge in. */
    public abstract val bridgePackage: Property<String>
}
