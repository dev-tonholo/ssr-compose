package dev.tonholo.ssrcompose.gradle

import javax.inject.Inject

/**
 * Setup of the app's font bundles, inside `ssrCompose { fonts { } }`. A server module declares each font bundle it
 * holds with [bundle].
 */
public abstract class FontSettings @Inject constructor() {
    internal val heldBundles: MutableMap<String, String> = linkedMapOf()

    /**
     * Declares one held font bundle: its [version] and the address of its `fonts.json`, a web address or a file path.
     *
     * @throws IllegalArgumentException when [version] is already declared.
     */
    public fun bundle(version: String, location: String) {
        require(version !in heldBundles) {
            "The font bundle \"$version\" is declared twice. Declare each version once."
        }
        heldBundles[version] = location
    }
}
