package dev.tonholo.ssrcompose.core.annotation

/**
 * Marks API that is public only for SSR Compose's own libraries, compiler-created code and hand-written bridges.
 * It can change without notice. Server screens and app code never need it. A hand-written bridge opts in with
 * `@OptIn(InternalSsrComposeApi::class)`.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Internal to SSR Compose: used by its own libraries, compiler-created code and hand-written " +
        "bridges, and it can change without notice. Opt in with @OptIn(InternalSsrComposeApi::class).",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class InternalSsrComposeApi
