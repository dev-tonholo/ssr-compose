package dev.tonholo.ssrcompose.contracttests.signature

import kotlin.metadata.KmClass
import kotlin.metadata.KmPackage
import kotlin.metadata.KmType
import kotlin.metadata.Visibility
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.visibility

/**
 * Reads the Kotlin metadata of compiled classes and reports every public declaration whose signature names a marked
 * type, at any depth of type arguments. Metadata keeps type arguments and tells `internal` from public, which the JVM
 * signature does not.
 */
internal class PublicSignatureScanner(private val markedTypes: MarkedTypes) {
    fun violationsIn(classes: List<Class<*>>): List<Violation> {
        val declared = classes.mapNotNull(::declaredOf)
        val kmClasses = declared
            .mapNotNull { declaredClass -> declaredClass.kmClass?.let { declaredClass.owner to it } }
            .toMap()
        return declared.flatMap { violationsOf(declared = it, kmClasses = kmClasses) }
    }

    private fun violationsOf(declared: DeclaredClass, kmClasses: Map<String, KmClass>): List<Violation> =
        when (val metadata = declared.metadata) {
            is KotlinClassMetadata.Class ->
                classViolations(declared = declared, kmClass = metadata.kmClass, kmClasses = kmClasses)
            is KotlinClassMetadata.FileFacade ->
                packageViolations(owner = declared.owner, kmPackage = metadata.kmPackage)
            is KotlinClassMetadata.MultiFileClassPart ->
                packageViolations(owner = declared.owner, kmPackage = metadata.kmPackage)
            else -> emptyList()
        }

    private fun classViolations(
        declared: DeclaredClass,
        kmClass: KmClass,
        kmClasses: Map<String, KmClass>,
    ): List<Violation> {
        if (!isExposed(owner = declared.owner, kmClasses = kmClasses)) return emptyList()
        val owner = declared.owner
        val constructors = kmClass.constructors.filter { it.visibility in EXPOSED }.map { constructor ->
            val types = constructor.valueParameters.flatMap(SignatureTypes::of)
            Signature(owner = owner, name = CONSTRUCTOR, types = types)
        }
        val functions = kmClass.functions.filter { it.visibility in EXPOSED }.map { function ->
            Signature(owner = owner, name = function.name, types = SignatureTypes.of(function))
        }
        val properties = kmClass.properties.filter { it.visibility in EXPOSED }.map { property ->
            Signature(owner = owner, name = property.name, types = SignatureTypes.of(property))
        }
        val shape = listOf(
            Signature(owner = owner, name = SUPERTYPES, types = kmClass.supertypes),
            Signature(
                owner = owner,
                name = TYPE_PARAMETERS,
                types = kmClass.typeParameters.flatMap(SignatureTypes::boundsOf),
            ),
        )
        return (constructors + functions + properties + shape).flatMap(::violationsOf)
    }

    private fun packageViolations(owner: String, kmPackage: KmPackage): List<Violation> {
        val functions = kmPackage.functions.filter { it.visibility in EXPOSED }.map { function ->
            Signature(owner = owner, name = function.name, types = SignatureTypes.of(function))
        }
        val properties = kmPackage.properties.filter { it.visibility in EXPOSED }.map { property ->
            Signature(owner = owner, name = property.name, types = SignatureTypes.of(property))
        }
        return (functions + properties).flatMap(::violationsOf)
    }

    private fun violationsOf(signature: Signature): List<Violation> =
        signature.types.flatMap(SignatureTypes::classNamesIn)
            .filter(markedTypes::marks)
            .distinct()
            .map { markedType -> Violation(declaration = signature.declaration, markedType = markedType) }

    private fun isExposed(owner: String, kmClasses: Map<String, KmClass>): Boolean {
        val kmClass = kmClasses[owner] ?: return true
        val outer = owner.substringBeforeLast(delimiter = ".", missingDelimiterValue = "")
        val outerExposed = outer.isEmpty() || isExposed(owner = outer, kmClasses = kmClasses)
        return kmClass.visibility in EXPOSED && outerExposed
    }

    private fun declaredOf(type: Class<*>): DeclaredClass? {
        val annotation = type.getAnnotation(Metadata::class.java) ?: return null
        return DeclaredClass(owner = type.metadataName(), metadata = KotlinClassMetadata.readStrict(annotation))
    }

    private class DeclaredClass(val owner: String, val metadata: KotlinClassMetadata) {
        val kmClass: KmClass? = (metadata as? KotlinClassMetadata.Class)?.kmClass
    }

    private class Signature(val owner: String, val name: String, val types: List<KmType>) {
        val declaration: String = "$owner.$name"
    }

    private companion object {
        const val CONSTRUCTOR = "<init>"
        const val SUPERTYPES = "<supertypes>"
        const val TYPE_PARAMETERS = "<type parameters>"
        val EXPOSED = setOf(Visibility.PUBLIC, Visibility.PROTECTED)
    }
}
