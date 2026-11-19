package dev.tonholo.ssrcompose.compiler.consumer

import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.validation.isKotlinIdentifier
import dev.tonholo.ssrcompose.compiler.SsrNames
import dev.tonholo.ssrcompose.compiler.consumer.slot.SlotParameterMapper
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.hasAnnotation

private const val PATH_SEPARATOR = "/"

/**
 * What one source file holds: its [source], the [choices] and [components] of its marked composables, and the
 * [parameterErrors] of those composables.
 */
internal class ScannedFile(
    val source: String,
    val choices: List<ChoiceList> = emptyList(),
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
            ?.let { CatalogFragment(source = source, choices = choices, components = components) }
}

/**
 * Finds the marked composables of a source file. A marked `expect` is left out and its marked `actual` is kept, so
 * a pair counts once.
 */
internal class MarkedComposables(private val session: FirSession) {
    @OptIn(DirectDeclarationsAccess::class)
    fun scan(file: FirFile): ScannedFile {
        val marked = file.declarations.filterIsInstance<FirNamedFunction>().filter(::isMarked)
        val errors = marked.flatMap(::errorsOf)
        val described = if (errors.isEmpty()) marked.map(::describe) else emptyList()
        return ScannedFile(
            source = file.sourcePath(),
            choices = described.flatMap { it.choices }.distinctBy { it.name },
            components = described.map { it.component },
            parameterErrors = errors,
        )
    }

    private fun isMarked(function: FirNamedFunction): Boolean =
        function.hasAnnotation(classId = SsrNames.consumerComposable, session = session) && !function.status.isExpect

    private fun describe(function: FirNamedFunction): DescribedComponent {
        val mapped = function.valueParameters
            .map { parameter -> ParameterKindMapper.map(parameter = parameter, session = session) }
            .filterIsInstance<ParameterMappingResult.Supported>()
        return DescribedComponent(
            component = ComponentDescriptor(
                name = function.name.asString(),
                parameters = mapped.map { it.parameter },
            ),
            choices = mapped.mapNotNull { it.choice },
        )
    }

    private fun errorsOf(function: FirNamedFunction): List<ConsumerError> {
        val composableName = function.name.asString()
        val nameError = if (isKotlinIdentifier(text = composableName)) {
            emptyList()
        } else {
            listOf(UnsupportedName(composable = composableName))
        }
        return nameError + parameterErrorsOf(function = function, composableName = composableName)
    }

    private fun parameterErrorsOf(function: FirNamedFunction, composableName: String): List<ConsumerError> =
        function.valueParameters.mapNotNull { parameter ->
            val parameterName = parameter.name.asString()
            when {
                !isKotlinIdentifier(text = parameterName) ->
                    UnsupportedName(composable = composableName, parameter = parameterName)
                SlotParameterMapper.hasConflictingAnnotations(parameter = parameter, session = session) ->
                    ConflictingSlotAnnotations(
                        composable = composableName,
                        parameter = parameterName,
                    )
                isUnsupported(parameter = parameter) ->
                    UnsupportedParameter(
                        composable = composableName,
                        parameter = parameterName,
                    )
                else -> null
            }
        }

    private fun isUnsupported(parameter: FirValueParameter): Boolean =
        ParameterKindMapper.map(parameter = parameter, session = session) is ParameterMappingResult.Unsupported

    private data class DescribedComponent(
        val component: ComponentDescriptor,
        val choices: List<ChoiceList>,
    )
}

private fun FirFile.sourcePath(): String {
    val packageFolders = packageDirective.packageFqName.pathSegments().map { it.asString() }
    return (packageFolders + name).joinToString(separator = PATH_SEPARATOR)
}
