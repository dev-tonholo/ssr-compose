package dev.tonholo.ssrcompose.tooling.fetch

import java.io.IOException

/** A location could not be reached: no such file, no answer, or an answer other than success. */
public class UnreachableLocationException(message: String, cause: Throwable? = null) : IOException(message, cause)
