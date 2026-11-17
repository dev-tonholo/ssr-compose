package dev.tonholo.ssrcompose.compiler.consumer.renderer

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Identifiers and naming rules for the consumer renderers the compiler plugin creates.
 */
internal object RendererNames {
    private const val FILE_PREFIX = "__SsrCompose_"
    private const val RENDERER_SUFFIX = "Renderer"
    private const val FILE_EXTENSION = "__.kt"
    private val consumerPackage: FqName = FqName("dev.tonholo.ssrcompose.consumer")
    private val scopeClassName: Name = Name.identifier("PlacedComponentScope")

    val placedComponentScope: ClassId = ClassId(packageFqName = consumerPackage, topLevelName = scopeClassName)
    val placedParameter: Name = Name.identifier("placed")
    val text: CallableId = CallableId(
        packageName = consumerPackage,
        className = FqName(scopeClassName.asString()),
        callableName = Name.identifier("text"),
    )

    /** The callable identifier of the renderer created for [composable]. */
    fun rendererCallableId(composable: CallableId): CallableId =
        CallableId(
            packageName = composable.packageName,
            callableName = Name.identifier("${composable.callableName.asString()}$RENDERER_SUFFIX"),
        )

    /** The synthetic file name of the renderer created for [composable], one file per composable. */
    fun containingFileName(composable: CallableId): String =
        "$FILE_PREFIX${composable.callableName.asString()}$RENDERER_SUFFIX$FILE_EXTENSION"
}
