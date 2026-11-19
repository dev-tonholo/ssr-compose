package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.catalog.validation.isKotlinPackageName
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import java.util.Locale

/** The largest bridge the JVM class file format can safely hold. Research note R16 has the measurements. */
internal object BridgeLimits {
    /** Keeps the parameter slots of the worst shape, every parameter optional and wide, within the JVM's 255. */
    const val MAX_PARAMETERS = 118

    /** Keeps the constant pool of a class of the largest bridges within the JVM's 65,534 entries. */
    const val MAX_FUNCTIONS_PER_CLASS = 150

    /** Keeps the folders of a bridge package within what file systems allow for one file name. */
    const val MAX_BRIDGE_PACKAGE_BYTES = 255

    private const val JVM_RESERVED_PACKAGE = "java"
    private const val LAST_DEVICE_NUMBER = 9
    private val WINDOWS_DEVICE_NAMES: Set<String> = setOf("CON", "PRN", "AUX", "NUL") +
        (1..LAST_DEVICE_NUMBER).flatMap { number -> listOf("COM$number", "LPT$number") }

    /** Returns why the option of a held catalog cannot be used. */
    fun optionProblems(option: CatalogOptionValue): List<String> = listOfNotNull(
        versionProblem(version = option.version),
        packageProblem(option = option),
    )

    /**
     * Returns one problem for each bridge package that more than one of [options] names. Case is ignored, because
     * packages that differ only in case write the same class files on a case-insensitive disk. Folding through the
     * upper case first also joins letters with two lower case forms, such as the Greek sigmas.
     */
    fun sharedPackageProblems(options: List<CatalogOptionValue>): List<String> =
        options.groupBy { it.bridgePackage.uppercase(Locale.ROOT).lowercase(Locale.ROOT) }
            .values
            .filter { sharing -> sharing.size > 1 }
            .map { sharing ->
                val versions = sharing.joinToString { option -> "'${option.version}'" }
                val spellings = sharing.map { it.bridgePackage }.distinct().joinToString { spelling -> "'$spelling'" }
                "The catalog versions $versions share the bridge package $spellings, ignoring case. " +
                    "Each held catalog needs its own bridge package."
            }

    /** Returns why a component of [parameterCount] parameters cannot get a bridge, or null when it can. */
    fun parameterRefusal(componentName: String, parameterCount: Int): String? {
        if (parameterCount <= MAX_PARAMETERS) return null
        return "The composable '$componentName' has $parameterCount parameters, " +
            "and a bridge can have at most $MAX_PARAMETERS."
    }

    /** Returns the refusal of each of [componentNames] whose bridge class would hold too many functions. */
    fun classRefusals(componentNames: List<String>, classRule: BridgeClassRule): Map<String, String> =
        componentNames.groupBy { name -> classRule.fileNameOf(componentName = name) }
            .filterValues { names -> names.size > MAX_FUNCTIONS_PER_CLASS }
            .flatMap { (fileName, names) ->
                names.map { name ->
                    name to "The composable '$name' cannot get a bridge: the bridge class ${fileName}Kt would hold " +
                        "${names.size} functions, and one class can hold at most $MAX_FUNCTIONS_PER_CLASS."
                }
            }
            .toMap()

    private fun versionProblem(version: String): String? =
        try {
            CatalogVersion(value = version)
            null
        } catch (exception: IllegalArgumentException) {
            "The catalog version option is not valid: ${exception.message}."
        }

    private fun packageProblem(option: CatalogOptionValue): String? {
        val bytes = option.bridgePackage.encodeToByteArray().size
        return when {
            bytes > MAX_BRIDGE_PACKAGE_BYTES ->
                "The bridge package of catalog version ${option.version} has $bytes bytes, " +
                    "and a bridge package can have at most $MAX_BRIDGE_PACKAGE_BYTES."

            !isKotlinPackageName(text = option.bridgePackage) ->
                "The bridge package '${option.bridgePackage}' of catalog version ${option.version} " +
                    "is not made of Kotlin identifiers separated by dots."

            else -> reservedPackageProblem(option = option)
        }
    }

    private fun reservedPackageProblem(option: CatalogOptionValue): String? {
        val segments = option.bridgePackage.split('.')
        val deviceName = segments.firstOrNull { segment -> segment.uppercase(Locale.ROOT) in WINDOWS_DEVICE_NAMES }
        val subject = "The bridge package '${option.bridgePackage}' of catalog version ${option.version}"
        return when {
            segments.first() == JVM_RESERVED_PACKAGE ->
                "$subject is inside the package '$JVM_RESERVED_PACKAGE', which the JVM keeps for itself."

            deviceName != null -> "$subject has the segment '$deviceName', which Windows reserves for a device."
            else -> null
        }
    }

}
