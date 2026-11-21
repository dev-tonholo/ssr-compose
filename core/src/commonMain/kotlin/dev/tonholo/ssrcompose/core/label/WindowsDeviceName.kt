package dev.tonholo.ssrcompose.core.label

private const val DEVICE_NUMBER_FIRST = 1
private const val DEVICE_NUMBER_LAST = 9
private val BASE_DEVICE_NAMES = setOf("con", "prn", "aux", "nul")
private val NUMBERED_DEVICE_PREFIXES = setOf("com", "lpt")

private val DEVICE_NAMES: Set<String> = BASE_DEVICE_NAMES + NUMBERED_DEVICE_PREFIXES.flatMap { prefix ->
    (DEVICE_NUMBER_FIRST..DEVICE_NUMBER_LAST).map { number -> "$prefix$number" }
}

/**
 * True when [name] is a name Windows keeps for a device, in any case: `con`, `prn`, `aux`, `nul`, `com1` to
 * `com9` or `lpt1` to `lpt9`. Windows cannot use such a name for a folder.
 */
internal fun isWindowsDeviceName(name: String): Boolean = name.lowercase() in DEVICE_NAMES
