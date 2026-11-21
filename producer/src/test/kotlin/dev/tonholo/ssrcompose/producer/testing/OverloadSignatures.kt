package dev.tonholo.ssrcompose.producer.testing

import java.lang.invoke.MethodType
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmFunction
import kotlin.metadata.KmType
import kotlin.metadata.KmTypeProjection
import kotlin.metadata.KmValueParameter
import kotlin.metadata.KmVariance
import kotlin.metadata.declaresDefaultValue
import kotlin.metadata.isNullable
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.annotations
import kotlin.metadata.jvm.signature

/** A parameter as Kotlin metadata describes it: its name, its type and whether it declares a default value. */
internal data class ParameterSignature(val name: String, val type: String, val hasDefault: Boolean)

/** The parameters of one overload, in order. */
internal data class OverloadSignature(val parameters: List<ParameterSignature>)

/**
 * The overloads named [functionName] that source code can call, from the file facade class [facadeClassName]. An
 * overload deprecated with level HIDDEN is not callable from source, the compiler writes it as a synthetic method,
 * and it is left out.
 */
internal fun callableOverloads(facadeClassName: String, functionName: String): List<OverloadSignature> {
    val facade = Class.forName(facadeClassName)
    val hiddenSignatures = facade.declaredMethods
        .filter { method -> method.isSynthetic }
        .map { method ->
            method.name + MethodType.methodType(method.returnType, method.parameterTypes).toMethodDescriptorString()
        }
        .toSet()
    return facade.kotlinFunctions()
        .filter { function -> function.name == functionName }
        .filterNot { function -> function.signature?.toString() in hiddenSignatures }
        .map { function ->
            OverloadSignature(parameters = function.valueParameters.map { parameter -> parameter.toSignature() })
        }
}

private fun Class<*>.kotlinFunctions(): List<KmFunction> {
    val metadata = checkNotNull(getAnnotation(Metadata::class.java)) { "$name has no Kotlin metadata" }
    val facade = KotlinClassMetadata.readLenient(metadata)
    return checkNotNull((facade as? KotlinClassMetadata.FileFacade)?.kmPackage?.functions) {
        "$name is not a file facade"
    }
}

private fun KmValueParameter.toSignature() =
    ParameterSignature(name = name, type = type.render(), hasDefault = declaresDefaultValue)

private fun KmType.render(): String {
    val annotationList = annotations.joinToString(separator = "") { annotation -> "@${annotation.className} " }
    val arguments = arguments.joinToString(separator = ",") { projection -> projection.render() }
    val argumentList = if (arguments.isEmpty()) "" else "<$arguments>"
    val nullability = if (isNullable) "?" else ""
    return "$annotationList${classifier.nameOf()}$argumentList$nullability"
}

private fun KmTypeProjection.render(): String {
    val projected = type ?: return "*"
    val varianceName = when (variance) {
        KmVariance.IN -> "in "
        KmVariance.OUT -> "out "
        KmVariance.INVARIANT, null -> ""
    }
    return varianceName + projected.render()
}

private fun KmClassifier.nameOf(): String = when (this) {
    is KmClassifier.Class -> name
    is KmClassifier.TypeAlias -> name
    is KmClassifier.TypeParameter -> "T$id"
}
