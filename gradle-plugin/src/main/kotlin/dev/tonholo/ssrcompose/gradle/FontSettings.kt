package dev.tonholo.ssrcompose.gradle

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Setup of the app's fonts, inside `ssrCompose { fonts { } }`, separate from the catalog. An app module sets
 * [version] and declares its families with [family]. A server module declares each font bundle it holds with
 * [bundle].
 */
public abstract class FontSettings @Inject constructor(private val objects: ObjectFactory) {
    internal val heldBundles: MutableMap<String, String> = linkedMapOf()
    internal val families: MutableList<FontFamilySettings> = mutableListOf()

    internal val hasFonts: Boolean
        get() = families.any { family -> family.fonts.isNotEmpty() }

    /** The version of the app's font bundle. Raise it whenever a declared font changes. */
    public abstract val version: Property<String>

    /**
     * Declares one font family the app draws server text with, and its files.
     *
     * @throws IllegalStateException when this module also declares a [bundle].
     */
    public fun family(name: String, action: Action<FontFamilySettings>) {
        check(heldBundles.isEmpty()) { MIXED_DECLARATIONS }
        val settings = objects.newInstance(FontFamilySettings::class.java, name)
        action.execute(settings)
        families += settings
    }

    /**
     * Declares one held font bundle: its [version] and the address of its `fonts.json`, a web address or a file path.
     *
     * @throws IllegalArgumentException when [version] is already declared.
     * @throws IllegalStateException when this module also declares a [family].
     */
    public fun bundle(version: String, location: String) {
        check(families.isEmpty()) { MIXED_DECLARATIONS }
        require(version !in heldBundles) {
            "The font bundle \"$version\" is declared twice. Declare each version once."
        }
        heldBundles[version] = location
    }

    private companion object {
        const val MIXED_DECLARATIONS =
            "bundle() belongs to the server module and family() to the app module. One module cannot declare both."
    }
}
