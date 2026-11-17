package dev.tonholo.ssrcompose.compiler.consumer

/**
 * A reason to fail the compilation of a consumer file. Every message names what is wrong and where.
 */
internal sealed interface ConsumerError {
    val message: String
}

/**
 * A parameter of a marked composable that is not text, which is all a fragment can describe so far.
 */
internal data class UnsupportedParameter(val composable: String, val parameter: String) : ConsumerError {
    override val message: String =
        "The parameter $parameter of the marked composable $composable is not text. " +
            "Only text parameters are supported so far"
}

/**
 * A source file that has marked composables but whose package folders and file name cannot be a fragment source.
 */
internal data class UnsupportedSource(val source: String) : ConsumerError {
    override val message: String =
        "The file $source has marked composables, but its package folders and file name cannot name a fragment: " +
            "no empty segment, no segment that starts or ends with a dot, no two dots in a row, and a file name " +
            "that fits a file system"
}
