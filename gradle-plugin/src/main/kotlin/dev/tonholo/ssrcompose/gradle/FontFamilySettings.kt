package dev.tonholo.ssrcompose.gradle

import javax.inject.Inject

/**
 * One font family of the app, inside `fonts { family(name) { } }`.
 *
 * @property name the family name server text names.
 */
public abstract class FontFamilySettings @Inject constructor(public val name: String) {
    internal val fonts: MutableList<DeclaredFont> = mutableListOf()

    /** Declares one file of the family: a `ttf` or `otf` [file], relative to the module, its [weight] and [italic]. */
    public fun font(file: String, weight: Int, italic: Boolean = false) {
        fonts += DeclaredFont(file = file, weight = weight, italic = italic)
    }
}
