package dev.tonholo.ssrcompose.compiler.consumer

import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.validation.isKotlinIdentifier
import dev.tonholo.ssrcompose.compiler.consumer.slot.SlotParameterMapper
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.getSingleMatchedExpectForActualOrNull
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

private const val PATH_SEPARATOR = "/"

/**
 * What one source file holds: its [source], the [choices] and [components] of its marked composables, and the
 * [declarationErrors] of the functions it marks and of the functions that cannot be marked.
 */
internal class ScannedFile(
    val source: String,
    val choices: List<ChoiceList> = emptyList(),
    val components: List<ComponentDescriptor>,
    val declarationErrors: List<ConsumerError>,
) {
    private val hasValidSource: Boolean = CatalogFragment.isValidSource(source = source)

    val errors: List<ConsumerError>
        get() = when {
            declarationErrors.isNotEmpty() -> declarationErrors
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
 * Finds the marked composables of a source file, and every function in it that is marked but cannot be, or must be
 * marked and is not. A marked `expect` is left out and its marked `actual` is kept, so a pair counts once.
 */
internal class MarkedComposables(private val session: FirSession) {
    private val markable = MarkableFunction(session = session)

    fun scan(file: FirFile): ScannedFile {
        val problems = namedFunctionsIn(file = file)
            .filter(markable::concerns)
            .associateWith { function -> markable.problemsOf(function = function) }
        val functionErrors = problems.flatMap { (function, found) ->
            found.map { problem ->
                UnmarkableFunction(
                    function = function.name.asString(),
                    problem = problem,
                    sourceElement = function.source,
                )
            }
        }
        val marked = problems.filterValues { it.isEmpty() }.keys.toList()
        val seenChoices = mutableMapOf<String, ChoiceList>()
        val errors = functionErrors + marked.flatMap { function ->
            errorsOf(function = function, seenChoices = seenChoices)
        }
        val described = if (errors.isEmpty()) marked.map(::describe) else emptyList()
        return ScannedFile(
            source = file.sourcePath(),
            choices = described.flatMap { it.choices }.distinctBy { it.name },
            components = described.map { it.component },
            declarationErrors = errors,
        )
    }

    private fun namedFunctionsIn(file: FirFile): List<FirNamedFunction> {
        val collector = NamedFunctionCollector()
        file.accept(collector)
        return collector.functions
    }

    private class NamedFunctionCollector : FirVisitorVoid() {
        val functions: MutableList<FirNamedFunction> = mutableListOf()

        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitNamedFunction(namedFunction: FirNamedFunction) {
            functions += namedFunction
            namedFunction.acceptChildren(this)
        }
    }

    private fun describe(function: FirNamedFunction): DescribedComponent {
        val defaults = hasDefaultValues(function = function)
        val mapped = function.valueParameters
            .mapIndexed { index, parameter ->
                ParameterKindMapper.map(parameter = parameter, session = session, hasDefault = defaults[index])
            }
            .filterIsInstance<ParameterMappingResult.Supported>()
        return DescribedComponent(
            component = ComponentDescriptor(
                name = function.name.asString(),
                parameters = mapped.map { it.parameter },
            ),
            choices = mapped.mapNotNull { it.choice },
        )
    }

    private fun hasDefaultValues(function: FirNamedFunction): List<Boolean> {
        val expectDefaults = matchedExpectOf(function = function)?.valueParameterSymbols?.map { it.hasDefaultValue }
        return function.valueParameters.mapIndexed { index, parameter ->
            expectDefaults?.getOrNull(index) ?: (parameter.defaultValue != null)
        }
    }

    private fun matchedExpectOf(function: FirNamedFunction): FirNamedFunctionSymbol? {
        if (!function.status.isActual) return null
        return function.symbol.getSingleMatchedExpectForActualOrNull() as? FirNamedFunctionSymbol
    }

    private fun errorsOf(
        function: FirNamedFunction,
        seenChoices: MutableMap<String, ChoiceList>,
    ): List<ConsumerError> {
        val composableName = function.name.asString()
        val nameError = if (isKotlinIdentifier(text = composableName)) {
            emptyList()
        } else {
            listOf(UnsupportedName(composable = composableName, sourceElement = function.source))
        }
        return nameError + parameterErrorsOf(
            function = function,
            composableName = composableName,
            seenChoices = seenChoices,
        )
    }

    private fun parameterErrorsOf(
        function: FirNamedFunction,
        composableName: String,
        seenChoices: MutableMap<String, ChoiceList>,
    ): List<ConsumerError> =
        function.valueParameters.mapNotNull { parameter ->
            parameterErrorOf(
                parameter = parameter,
                composableName = composableName,
                seenChoices = seenChoices,
            )
        }

    private fun parameterErrorOf(
        parameter: FirValueParameter,
        composableName: String,
        seenChoices: MutableMap<String, ChoiceList>,
    ): ConsumerError? {
        val parameterName = parameter.name.asString()
        return when {
            !isKotlinIdentifier(text = parameterName) ->
                UnsupportedName(
                    composable = composableName,
                    parameter = parameterName,
                    sourceElement = parameter.source,
                )
            SlotParameterMapper.hasConflictingAnnotations(parameter = parameter, session = session) ->
                ConflictingSlotAnnotations(
                    composable = composableName,
                    parameter = parameterName,
                    sourceElement = parameter.source,
                )
            else -> mappedParameterErrorOrNull(
                parameter = parameter,
                composableName = composableName,
                parameterName = parameterName,
                seenChoices = seenChoices,
            )
        }
    }

    private fun mappedParameterErrorOrNull(
        parameter: FirValueParameter,
        composableName: String,
        parameterName: String,
        seenChoices: MutableMap<String, ChoiceList>,
    ): UnsupportedParameter? =
        when (val result = ParameterKindMapper.map(parameter = parameter, session = session)) {
            is ParameterMappingResult.Unsupported -> UnsupportedParameter(
                composable = composableName,
                parameter = parameterName,
                reason = result.reason,
                sourceElement = parameter.source,
            )
            is ParameterMappingResult.Supported -> conflictingChoiceErrorOrNull(
                choice = result.choice,
                parameter = parameter,
                composableName = composableName,
                parameterName = parameterName,
                seenChoices = seenChoices,
            )
            ParameterMappingResult.Skipped -> null
        }

    private fun conflictingChoiceErrorOrNull(
        choice: ChoiceList?,
        parameter: FirValueParameter,
        composableName: String,
        parameterName: String,
        seenChoices: MutableMap<String, ChoiceList>,
    ): UnsupportedParameter? {
        val previous = choice?.let { seenChoices.putIfAbsent(it.name, it) }
        return choice?.takeIf { previous != null && previous != it }?.let { conflicting ->
            UnsupportedParameter(
                composable = composableName,
                parameter = parameterName,
                reason = "the choice list ${conflicting.name} differs from another enum with the same name: " +
                    "two different enums with that name are used in the same file",
                sourceElement = parameter.source,
            )
        }
    }

    private data class DescribedComponent(
        val component: ComponentDescriptor,
        val choices: List<ChoiceList>,
    )
}

private fun FirFile.sourcePath(): String {
    val packageFolders = packageDirective.packageFqName.pathSegments().map { it.asString() }
    return (packageFolders + name).joinToString(separator = PATH_SEPARATOR)
}
