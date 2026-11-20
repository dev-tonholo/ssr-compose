package dev.tonholo.ssrcompose.core.annotation

/**
 * Marks API that exists for the library itself and for compiler-created and hand-written bridges: the wire types and
 * the bridge entry points. App and server developers never call these declarations. A hand-written bridge opts in with
 * `@OptIn(InternalSsrComposeApi::class)`.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Intended for the SSR Compose library and for compiler-generated and hand-written SSR Compose bridges. " +
        "Opt in explicitly with @OptIn(InternalSsrComposeApi::class) if calling by hand.",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class InternalSsrComposeApi
