package dev.tonholo.ssrcompose.compiler.consumer

import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

private const val PATH_SEPARATOR = "/"

private val CONSUMER_COMPOSABLE: ClassId =
    ClassId.topLevel(topLevelFqName = FqName(ConsumerComposable::class.java.name))

/**
 * What one source file holds: its [source], the [components] of its marked composables, and the [unsupported]
 * parameters of those composables.
 */
internal class ScannedFile(
    val source: String,
    val components: List<ComponentDescriptor>,
    val unsupported: List<UnsupportedParameter>,
) {
    private val hasValidSource: Boolean = CatalogFragment.isValidSource(source = source)

    val errors: List<ConsumerError>
        get() = when {
            unsupported.isNotEmpty() -> unsupported
            components.isNotEmpty() && !hasValidSource -> listOf(UnsupportedSource(source = source))
            else -> emptyList()
        }

    /**
     * The fragment of the file, or null when the file has an error or its source cannot name a fragment, which
     * is only possible for a file with no marked composable.
     */
    fun fragmentOrNull(): CatalogFragment? =
        takeIf { errors.isEmpty() && hasValidSource }
            ?.let { CatalogFragment(source = source, components = components) }
}

/**
 * Finds the marked composables of a source file. A marked `expect` is left out and its marked `actual` is kept, so
 * a pair counts once.
 */
internal class MarkedComposables(private val session: FirSession) {
    @OptIn(DirectDeclarationsAccess::class)
    fun scan(file: FirFile): ScannedFile {
        val marked = file.declarations.filterIsInstance<FirNamedFunction>().filter(::isMarked)
        return ScannedFile(
            source = file.sourcePath(),
            components = marked.map(::describe),
            unsupported = marked.flatMap(::unsupportedParametersOf),
        )
    }

    private fun isMarked(function: FirNamedFunction): Boolean =
        function.hasAnnotation(classId = CONSUMER_COMPOSABLE, session = session) && !function.status.isExpect

    private fun describe(function: FirNamedFunction): ComponentDescriptor =
        ComponentDescriptor(
            name = function.name.asString(),
            parameters = function.valueParameters
                .filter(ParameterKindMapper::isSupported)
                .map(ParameterKindMapper::toParameter),
        )

    private fun unsupportedParametersOf(function: FirNamedFunction): List<UnsupportedParameter> =
        function.valueParameters.filterNot(ParameterKindMapper::isSupported).map { parameter ->
            UnsupportedParameter(composable = function.name.asString(), parameter = parameter.name.asString())
        }
}

private fun FirFile.sourcePath(): String {
    val packageFolders = packageDirective.packageFqName.pathSegments().map { it.asString() }
    return (packageFolders + name).joinToString(separator = PATH_SEPARATOR)
}
