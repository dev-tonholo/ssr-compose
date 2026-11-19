package dev.tonholo.ssrcompose.compiler.producer.scan

/** Tells which component names the code of a module may use. */
internal fun interface ComponentNameSource {
    /** Returns the [names] that appear in the code. It may return a name the code does not call, but never miss one. */
    fun namedAmong(names: Set<String>): Set<String>
}
