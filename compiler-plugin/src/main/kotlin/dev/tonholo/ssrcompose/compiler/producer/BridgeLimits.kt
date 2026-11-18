package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.catalog.validation.isKotlinPackageName
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import java.util.Locale

/** The largest bridge the JVM class file format can safely hold. Research note R16 has the measurements. */
internal object BridgeLimits {
    /** Keeps the parameter slots of the worst shape, every parameter optional and wide, within the JVM's 255. */
    const val MAX_PARAMETERS = 118

    /** Keeps the constant pool of a class of the largest bridges within the JVM's 65,535 entries. */
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

    /** Returns every limit the bridges of [components] break. */
    fun componentProblems(components: List<BridgeComponent>): List<String> =
        components.mapNotNull(::parameterProblem) + classProblems(components = components)

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

    private fun parameterProblem(component: BridgeComponent): String? {
        val count = component.parameters.size
        if (count <= MAX_PARAMETERS) return null
        return "The composable '${component.name}' has $count parameters, " +
            "and a bridge can have at most $MAX_PARAMETERS."
    }

    private fun classProblems(components: List<BridgeComponent>): List<String> =
        components.groupingBy { BridgeClassRule.fileNameOf(componentName = it.name) }
            .eachCount()
            .filterValues { count -> count > MAX_FUNCTIONS_PER_CLASS }
            .map { (fileName, count) ->
                "The bridge class ${fileName}Kt would hold $count functions, " +
                    "and one class can hold at most $MAX_FUNCTIONS_PER_CLASS."
            }
}
