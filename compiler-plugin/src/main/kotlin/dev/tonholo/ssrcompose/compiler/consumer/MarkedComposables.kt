package dev.tonholo.ssrcompose.compiler.consumer

import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.compiler.SsrNames
import dev.tonholo.ssrcompose.compiler.consumer.slot.SlotParameterMapper
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation

private const val PATH_SEPARATOR = "/"

/**
 * What one source file holds: its [source], the [components] of its marked composables, and the [parameterErrors]
 * of those composables.
 */
internal class ScannedFile(
    val source: String,
    val components: List<ComponentDescriptor>,
    val parameterErrors: List<ConsumerError>,
) {
    private val hasValidSource: Boolean = CatalogFragment.isValidSource(source = source)

    val errors: List<ConsumerError>
        get() = when {
            parameterErrors.isNotEmpty() -> parameterErrors
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
            parameterErrors = marked.flatMap(::parameterErrorsOf),
        )
    }

    private fun isMarked(function: FirNamedFunction): Boolean =
        function.hasAnnotation(classId = SsrNames.consumerComposable, session = session) && !function.status.isExpect

    private fun describe(function: FirNamedFunction): ComponentDescriptor =
        ComponentDescriptor(
            name = function.name.asString(),
            parameters = function.valueParameters
                .filter { parameter -> ParameterKindMapper.isSupported(parameter = parameter, session = session) }
                .map { parameter -> ParameterKindMapper.toParameter(parameter = parameter, session = session) },
        )

    private fun parameterErrorsOf(function: FirNamedFunction): List<ConsumerError> =
        function.valueParameters.mapNotNull { parameter ->
            when {
                SlotParameterMapper.hasConflictingAnnotations(parameter = parameter, session = session) ->
                    ConflictingSlotAnnotations(
                        composable = function.name.asString(),
                        parameter = parameter.name.asString(),
                    )
                !ParameterKindMapper.isSupported(parameter = parameter, session = session) ->
                    UnsupportedParameter(
                        composable = function.name.asString(),
                        parameter = parameter.name.asString(),
                    )
                else -> null
            }
        }
}

private fun FirFile.sourcePath(): String {
    val packageFolders = packageDirective.packageFqName.pathSegments().map { it.asString() }
    return (packageFolders + name).joinToString(separator = PATH_SEPARATOR)
}
