package dev.tonholo.ssrcompose.producer

/**
 * Marks producer API that exists for compiler-created and hand-written bridges only. Server screens call the bridge
 * functions, never these declarations. A hand-written bridge opts in with `@OptIn(InternalSsrComposeApi::class)`.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Intended for compiler-generated and hand-written SSR Compose bridges. " +
        "Opt in explicitly with @OptIn(InternalSsrComposeApi::class) if calling by hand.",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class InternalSsrComposeApi
