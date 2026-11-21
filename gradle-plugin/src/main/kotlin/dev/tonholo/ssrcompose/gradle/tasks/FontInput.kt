package dev.tonholo.ssrcompose.gradle.tasks

import dev.tonholo.ssrcompose.tooling.font.FontDeclaration
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * One declared font as a task input: its bytes and file name, and its family, weight and italic, so two files
 * swapped between weights are a change.
 */
internal abstract class FontInput {
    @get:Input
    abstract val family: Property<String>

    @get:Input
    abstract val weight: Property<Int>

    @get:Input
    abstract val italic: Property<Boolean>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val file: RegularFileProperty

    fun toDeclaration(): FontDeclaration =
        FontDeclaration(family = family.get(), weight = weight.get(), italic = italic.get(), file = file.get().asFile)
}
