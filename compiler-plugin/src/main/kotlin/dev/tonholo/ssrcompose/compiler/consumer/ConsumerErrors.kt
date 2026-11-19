package dev.tonholo.ssrcompose.compiler.consumer

import dev.tonholo.ssrcompose.catalog.validation.CatalogLimits
import org.jetbrains.kotlin.KtSourceElement

/**
 * A reason to fail the compilation of a consumer file. Every message names what is wrong and where.
 */
internal sealed interface ConsumerError {
    val message: String
    val sourceElement: KtSourceElement?
}

/**
 * A marked composable or parameter whose name is not a plain Kotlin identifier of at most
 * [CatalogLimits.MAX_NAME_LENGTH] characters, which is all the catalog can hold.
 */
internal data class UnsupportedName(
    val composable: String,
    val parameter: String? = null,
    override val sourceElement: KtSourceElement? = null,
) : ConsumerError {
    override val message: String = if (parameter == null) {
        "The marked composable $composable has a name the catalog cannot hold. " +
            "A name must be a plain Kotlin identifier of at most ${CatalogLimits.MAX_NAME_LENGTH} characters"
    } else {
        "The parameter $parameter of the marked composable $composable has a name the catalog cannot hold. " +
            "A name must be a plain Kotlin identifier of at most ${CatalogLimits.MAX_NAME_LENGTH} characters"
    }
}

/**
 * A parameter of a marked composable that the catalog cannot describe.
 */
internal data class UnsupportedParameter(
    val composable: String,
    val parameter: String,
    val reason: String,
    override val sourceElement: KtSourceElement? = null,
) : ConsumerError {
    override val message: String =
        "The parameter $parameter of the marked composable $composable cannot be described in the catalog: $reason"
}

/**
 * A slot parameter of a marked composable whose type has both `@ConsumerComposable` and `@ProducerComposable`.
 */
internal data class ConflictingSlotAnnotations(
    val composable: String,
    val parameter: String,
    override val sourceElement: KtSourceElement? = null,
) : ConsumerError {
    override val message: String =
        "The slot $parameter of the marked composable $composable has both @ConsumerComposable and " +
            "@ProducerComposable. A slot accepts consumer composables, producer composables or either, not both"
}

/**
 * A source file that has marked composables but whose package folders and file name cannot be a fragment source.
 */
internal data class UnsupportedSource(
    val source: String,
    override val sourceElement: KtSourceElement? = null,
) : ConsumerError {
    override val message: String =
        "The file $source has marked composables, but its package folders and file name cannot name a fragment: " +
            "no empty segment, no segment that starts or ends with a dot, no two dots in a row, and a file name " +
            "that fits a file system"
}
