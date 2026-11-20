package dev.tonholo.ssrcompose.contracttests.signature

import kotlin.metadata.KmClassifier
import kotlin.metadata.KmFunction
import kotlin.metadata.KmProperty
import kotlin.metadata.KmType
import kotlin.metadata.KmTypeParameter
import kotlin.metadata.KmValueParameter

/**
 * The types a declaration puts in its signature, and the class names inside a type at any depth.
 */
internal object SignatureTypes {
    @OptIn(ExperimentalContextParameters::class)
    fun of(function: KmFunction): List<KmType> =
        function.valueParameters.flatMap(::of) +
            function.returnType +
            listOfNotNull(function.receiverParameterType) +
            function.contextParameters.flatMap(::of) +
            function.typeParameters.flatMap(::boundsOf)

    @OptIn(ExperimentalContextParameters::class)
    fun of(property: KmProperty): List<KmType> =
        listOf(property.returnType) +
            listOfNotNull(property.receiverParameterType) +
            property.contextParameters.flatMap(::of) +
            property.typeParameters.flatMap(::boundsOf)

    fun of(parameter: KmValueParameter): List<KmType> =
        listOf(parameter.type)

    fun boundsOf(parameter: KmTypeParameter): List<KmType> = parameter.upperBounds

    fun classNamesIn(type: KmType): List<String> {
        val own = when (val classifier = type.classifier) {
            is KmClassifier.Class -> listOf(classifier.name)
            is KmClassifier.TypeAlias -> listOf(classifier.name)
            is KmClassifier.TypeParameter -> emptyList()
        }
        val nested = type.arguments.mapNotNull { it.type } +
            listOfNotNull(type.abbreviatedType, type.outerType)
        return own + nested.flatMap(::classNamesIn)
    }
}
