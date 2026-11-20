package dev.tonholo.ssrcompose.contracttests.signature

/** The name Kotlin metadata gives this class: slashes between packages and a dot before a nested class. */
internal fun Class<*>.metadataName(): String =
    name.replace(oldChar = '.', newChar = '/').replace(oldChar = '$', newChar = '.')
