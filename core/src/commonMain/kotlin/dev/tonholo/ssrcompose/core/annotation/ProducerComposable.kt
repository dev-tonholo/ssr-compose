package dev.tonholo.ssrcompose.core.annotation

/**
 * Marks a composable function, or a slot type, that the producer server owns.
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPEALIAS,
)
@Retention(AnnotationRetention.BINARY)
public annotation class ProducerComposable
