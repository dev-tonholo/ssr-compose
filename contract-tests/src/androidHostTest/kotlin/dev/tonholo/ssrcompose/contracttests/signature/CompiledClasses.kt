package dev.tonholo.ssrcompose.contracttests.signature

import java.io.File
import java.util.jar.JarFile

/**
 * Finds the compiled classes of a package, and of its sub packages, on the test classpath.
 */
internal object CompiledClasses {
    private const val CLASS_FILE_SUFFIX = ".class"
    private const val PACKAGE_INFO = "package-info"
    private const val MODULE_INFO = "module-info"

    /**
     * Loads, without initializing, every class of [packageName] and of its sub packages.
     * @throws IllegalStateException when the classpath holds none, so a check that reads nothing cannot pass.
     */
    fun inPackage(packageName: String): List<Class<*>> {
        val classLoader = checkNotNull(CompiledClasses::class.java.classLoader)
        val classes = classNamesInPackage(packageName = packageName)
            .map { className -> Class.forName(className, false, classLoader) }
        check(classes.isNotEmpty()) { "No compiled class of package $packageName is on the test classpath" }
        return classes
    }

    private fun classNamesInPackage(packageName: String): List<String> {
        val packagePath = packageName.replace(oldChar = '.', newChar = '/') + "/"
        return classpathEntries()
            .flatMap { entry -> if (entry.isDirectory) entry.classPathsInFolder() else entry.classPathsInJar() }
            .filter { path -> path.startsWith(packagePath) && path.endsWith(CLASS_FILE_SUFFIX) }
            .map { path -> path.removeSuffix(CLASS_FILE_SUFFIX).replace(oldChar = '/', newChar = '.') }
            .filterNot { name -> name.endsWith(PACKAGE_INFO) || name.endsWith(MODULE_INFO) }
            .distinct()
    }

    private fun classpathEntries(): List<File> =
        checkNotNull(System.getProperty("java.class.path"))
            .split(File.pathSeparator)
            .filter { it.isNotEmpty() }
            .map(::File)

    private fun File.classPathsInFolder(): List<String> =
        walkTopDown().filter { it.isFile }.map { it.relativeTo(base = this).invariantSeparatorsPath }.toList()

    private fun File.classPathsInJar(): List<String> =
        if (isFile && extension == "jar") {
            JarFile(this).use { jar -> jar.entries().toList().map { it.name } }
        } else {
            emptyList()
        }
}
