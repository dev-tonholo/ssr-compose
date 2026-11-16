package dev.tonholo.ssrcompose.probe

import java.nio.file.Path
import kotlin.io.path.relativeTo
import org.jetbrains.amper.plugins.TaskAction

@TaskAction
fun printModuleLocation(moduleName: String, moduleRootDir: String, projectRootDir: String) {
    val relativePath = Path.of(moduleRootDir).relativeTo(Path.of(projectRootDir))
    println("moduleName=$moduleName relativePath='$relativePath'")
}
