package dev.tonholo.ssrcompose.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

/**
 * Setup of the app project, inside `ssrCompose { consumer { } }`.
 */
public abstract class ConsumerSettings {
    /** The version of the catalog the app builds. Set it in the module that bundles the catalog. */
    public abstract val catalogVersion: Property<String>

    /** The folder that holds the saved catalogs. */
    public abstract val catalogDir: DirectoryProperty

    /** The name of this module's part of the catalog. */
    public abstract val partName: Property<String>
}
