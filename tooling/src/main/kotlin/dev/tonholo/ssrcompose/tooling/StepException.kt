package dev.tonholo.ssrcompose.tooling

/**
 * A build step stopped. The message says what failed and, where it can, what the developer runs next.
 */
public class StepException(message: String, cause: Throwable? = null) : Exception(message, cause)
