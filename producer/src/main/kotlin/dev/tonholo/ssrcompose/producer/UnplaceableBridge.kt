package dev.tonholo.ssrcompose.producer

import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi

/**
 * Marks a created bridge function whose composable cannot be placed from the server. Plugin-internal: the compiler
 * plugin puts it on the function and reads it back to fail every use of that function. Do not write it in code.
 *
 * @property message the error the compiler plugin reports for each use of the function.
 */
@InternalSsrComposeApi
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class UnplaceableBridge(public val message: String)
