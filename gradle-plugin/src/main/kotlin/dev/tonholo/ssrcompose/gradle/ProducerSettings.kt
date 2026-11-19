package dev.tonholo.ssrcompose.gradle

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * Setup of the server project, inside `ssrCompose { producer { } }`.
 */
public abstract class ProducerSettings @Inject constructor(private val objects: ObjectFactory) {
    internal val heldCatalogs: MutableList<HeldCatalogSettings> = mutableListOf()

    /** Declares one held catalog: its [version], the address of its manifest and the package of its bridge. */
    public fun catalog(version: String, action: Action<HeldCatalogSettings>) {
        val settings = objects.newInstance(HeldCatalogSettings::class.java, version)
        action.execute(settings)
        heldCatalogs += settings
    }
}
