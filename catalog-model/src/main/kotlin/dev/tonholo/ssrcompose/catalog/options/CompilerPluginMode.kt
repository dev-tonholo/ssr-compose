package dev.tonholo.ssrcompose.catalog.options

/**
 * The side of the connection a compilation belongs to.
 *
 * @property optionValue the text of the `mode` option of the compiler plugin.
 */
public enum class CompilerPluginMode(public val optionValue: String) {
    Consumer(optionValue = "consumer"),
    Producer(optionValue = "producer"),
    ;

    public companion object {
        /** Returns the mode written as [optionValue], or null when no mode has that text. */
        public fun fromOptionValue(optionValue: String): CompilerPluginMode? =
            entries.firstOrNull { mode -> mode.optionValue == optionValue }
    }
}
