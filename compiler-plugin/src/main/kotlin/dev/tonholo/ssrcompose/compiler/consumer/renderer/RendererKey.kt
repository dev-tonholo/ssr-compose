package dev.tonholo.ssrcompose.compiler.consumer.renderer

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/**
 * One regular value parameter of a marked consumer composable, pinned by its [name] and [type].
 */
internal data class RendererParameter(
    val name: Name,
    val type: ClassId,
)

/**
 * Marks a consumer renderer function created by the plugin and links it to the [composable] and [parameters] it
 * renders.
 */
internal data class RendererKey(
    val composable: CallableId,
    val parameters: List<RendererParameter>,
) : GeneratedDeclarationKey()
