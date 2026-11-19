@file:OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)

package dev.tonholo.ssrcompose.compiler.testing.scale

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.compiler.producer.BridgeCatalog
import dev.tonholo.ssrcompose.compiler.producer.BridgeCatalogLoader
import dev.tonholo.ssrcompose.compiler.producer.fir.BridgeDeclarationGenerator
import dev.tonholo.ssrcompose.compiler.producer.fir.UnplaceableBridgeCheckers
import dev.tonholo.ssrcompose.compiler.producer.ir.BridgeIrGenerationExtension
import dev.tonholo.ssrcompose.compiler.producer.scan.ComponentNameSource
import dev.tonholo.ssrcompose.compiler.producer.scan.ScanRoots
import dev.tonholo.ssrcompose.compiler.producer.scan.SourceNameScan
import java.io.File
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName

/** Runs the production steps of the producer side of the plugin, and times each one on [clock]. */
internal class MeasuredBridgeRegistrar(
    private val catalogs: List<CatalogOptionValue>,
    private val sourceFolder: File,
    private val clock: PluginStepClock,
) : CompilerPluginRegistrar() {
    override val pluginId: String = "dev.tonholo.ssrcompose.scale.measured"

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val roots = ScanRoots.of(
            sourceFolders = listOf(sourceFolder),
            compilerRoots = configuration.kotlinSourceRoots.map { root -> File(root.path) },
        )
        val loader = BridgeCatalogLoader(
            nameSource = MeasuredNameSource(delegate = SourceNameScan(roots = roots), clock = clock),
            moduleName = checkNotNull(configuration[CommonConfigurationKeys.MODULE_NAME]),
        )
        clock.start(step = PluginStep.ReadIndexes)
        val load = loader.load(options = catalogs)
        clock.stop(step = PluginStep.DecodeNamedComponents)
        check(load.problems.isEmpty()) { load.problems.joinToString(separator = "\n") }
        FirExtensionRegistrarAdapter.registerExtension(MeasuredFirRegistrar(catalogs = load.catalogs, clock = clock))
        IrGenerationExtension.registerExtension(
            MeasuredIrExtension(delegate = BridgeIrGenerationExtension(catalogs = load.catalogs), clock = clock),
        )
    }
}

/** The loader reads every index, asks for the names, then decodes, so the scan splits the load into its three steps. */
private class MeasuredNameSource(
    private val delegate: ComponentNameSource,
    private val clock: PluginStepClock,
) : ComponentNameSource {
    override fun namedAmong(names: Set<String>): Set<String> {
        clock.stop(step = PluginStep.ReadIndexes)
        val named = clock.measure(step = PluginStep.ScanSources) { delegate.namedAmong(names = names) }
        clock.start(step = PluginStep.DecodeNamedComponents)
        return named
    }
}

private class MeasuredFirRegistrar(
    private val catalogs: List<BridgeCatalog>,
    private val clock: PluginStepClock,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        catalogs.forEach { catalog ->
            +FirDeclarationGenerationExtension.Factory { session ->
                MeasuredBridgeGenerator(session = session, catalog = catalog, clock = clock)
            }
        }
        +FirAdditionalCheckersExtension.Factory { session -> MeasuredCallChecks(session = session, clock = clock) }
    }
}

private class MeasuredBridgeGenerator(
    session: FirSession,
    catalog: BridgeCatalog,
    private val clock: PluginStepClock,
) : FirDeclarationGenerationExtension(session) {
    private val bridges = BridgeDeclarationGenerator(session = session, catalog = catalog)

    override fun hasPackage(packageFqName: FqName): Boolean =
        clock.measure(step = PluginStep.DeclareBridges) { bridges.hasPackage(packageFqName = packageFqName) }

    override fun getTopLevelCallableIds(): Set<CallableId> =
        clock.measure(step = PluginStep.DeclareBridges) { bridges.getTopLevelCallableIds() }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> =
        clock.measure(step = PluginStep.DeclareBridges) {
            bridges.generateFunctions(callableId = callableId, context = context)
        }
}

private class MeasuredCallChecks(
    session: FirSession,
    private val clock: PluginStepClock,
) : FirAdditionalCheckersExtension(session) {
    private val checks = UnplaceableBridgeCheckers(session = session).expressionCheckers

    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers = checks.functionCallCheckers.mapTo(destination = HashSet()) { checker ->
            MeasuredChecker(delegate = checker, clock = clock)
        }
        override val callableReferenceAccessCheckers =
            checks.callableReferenceAccessCheckers.mapTo(destination = HashSet()) { checker ->
                MeasuredChecker(delegate = checker, clock = clock)
            }
    }
}

private class MeasuredChecker<E : FirStatement>(
    private val delegate: FirExpressionChecker<E>,
    private val clock: PluginStepClock,
) : FirExpressionChecker<E>(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: E) {
        clock.measure(step = PluginStep.CheckCalls) { delegate.check(expression = expression) }
    }
}

private class MeasuredIrExtension(
    private val delegate: IrGenerationExtension,
    private val clock: PluginStepClock,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        clock.measure(step = PluginStep.CreateBridgeBodies) {
            delegate.generate(moduleFragment = moduleFragment, pluginContext = pluginContext)
        }
    }
}
