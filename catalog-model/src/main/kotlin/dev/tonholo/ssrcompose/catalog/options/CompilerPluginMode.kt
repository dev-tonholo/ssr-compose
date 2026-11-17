package dev.tonholo.ssrcompose.catalog.options

/**
 * What the compiler plugin does in a compilation: the side of the connection it belongs to, or a check of calls to
 * the bridge of another module.
 *
 * @property optionValue the text of the `mode` option of the compiler plugin.
 */
public enum class CompilerPluginMode(public val optionValue: String) {
    Consumer(optionValue = "consumer"),
    Producer(optionValue = "producer"),
    Check(optionValue = "check"),
    ;

    public companion object {
        /** Returns the mode written as [optionValue], or null when no mode has that text. */
        public fun fromOptionValue(optionValue: String): CompilerPluginMode? =
            entries.firstOrNull { mode -> mode.optionValue == optionValue }
    }
}
