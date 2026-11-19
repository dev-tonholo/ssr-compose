package dev.tonholo.ssrcompose.producer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import app.cash.turbine.test
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.handshake.AppIdentity
import dev.tonholo.ssrcompose.core.handshake.DeviceContext
import dev.tonholo.ssrcompose.core.handshake.Theme
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import dev.tonholo.ssrcompose.core.message.ScreenContent
import dev.tonholo.ssrcompose.producer.internal.LiveScreen
import dev.tonholo.ssrcompose.producer.internal.PlacedNode
import dev.tonholo.ssrcompose.producer.internal.ScreenApplier
import dev.tonholo.ssrcompose.producer.internal.SnapshotNotifier
import dev.tonholo.ssrcompose.producer.internal.content.toScreenContent
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class ConsumerComponentTest {
    @Test
    fun `given components placed in order, when composed, then nodes carry names and values in call order`() {
        // Arrange
        val expectedNames = listOf(FIRST_COMPONENT, SECOND_COMPONENT)
        val expectedValues = listOf(
            listOf(TITLE_PARAMETER to FIRST_TITLE, SUBTITLE_PARAMETER to FIRST_SUBTITLE),
            listOf(TITLE_PARAMETER to SECOND_TITLE),
        )

        // Act
        val components = composeOnce(
            content = {
                TestComponent(
                    name = FIRST_COMPONENT,
                    values = listOf(TITLE_PARAMETER to FIRST_TITLE, SUBTITLE_PARAMETER to FIRST_SUBTITLE),
                )
                TestComponent(name = SECOND_COMPONENT, values = listOf(TITLE_PARAMETER to SECOND_TITLE))
            },
            observe = { root -> root.children.filterIsInstance<PlacedNode.Component>() },
        )

        // Assert
        assertEquals(expected = expectedNames, actual = components.map { it.name })
        assertEquals(
            expected = expectedValues,
            actual = components.map { it.values.entries.map { entry -> entry.toPair() } },
        )
    }

    @Test
    fun `given components from two bridges, when composed, then each node carries its bridge catalog version`() {
        // Arrange
        val expectedVersions = listOf(FIRST_CATALOG_VERSION, SECOND_CATALOG_VERSION)

        // Act
        val components = composeOnce(
            content = {
                TestComponent(
                    catalogVersion = FIRST_CATALOG_VERSION,
                    name = FIRST_COMPONENT,
                    values = listOf(TITLE_PARAMETER to FIRST_TITLE),
                )
                TestComponent(
                    catalogVersion = SECOND_CATALOG_VERSION,
                    name = SECOND_COMPONENT,
                    values = listOf(TITLE_PARAMETER to SECOND_TITLE),
                )
            },
            observe = { root -> root.children.filterIsInstance<PlacedNode.Component>() },
        )

        // Assert
        assertEquals(expected = expectedVersions, actual = components.map { it.catalogVersion })
    }

    @Test
    fun `given a value parameter that was not given, when composed, then that parameter is absent`() {
        // Arrange
        val expectedValues = mapOf(TITLE_PARAMETER to FIRST_TITLE)

        // Act
        val component = composeOnce(
            content = {
                TestComponent(
                    name = FIRST_COMPONENT,
                    values = listOf(TITLE_PARAMETER to FIRST_TITLE, SUBTITLE_PARAMETER to null),
                )
            },
            observe = { root -> root.children.filterIsInstance<PlacedNode.Component>().single() },
        )

        // Assert
        assertEquals(expected = expectedValues, actual = component.values)
    }

    @Test
    fun `given a slot parameter that was not given, when composed, then that slot is absent`() {
        // Arrange
        val expectedSlots = listOf(HEADER_SLOT)

        // Act
        val component = composeOnce(
            content = {
                TestComponent(
                    name = FIRST_COMPONENT,
                    slots = listOf(HEADER_SLOT to { DoneFooter() }, FOOTER_SLOT to null),
                )
            },
            observe = { root -> root.children.filterIsInstance<PlacedNode.Component>().single() },
        )

        // Assert
        assertEquals(
            expected = expectedSlots,
            actual = component.children.filterIsInstance<PlacedNode.Slot>().map { it.name },
        )
    }

    @Test
    fun `given placed components, when converted to screen content, then order is kept and omitted ones are absent`() {
        // Arrange
        val expected = expectedMultiComponentScreen()

        // Act
        val actual = composeOnce(
            content = { MultiComponentScreen() },
            observe = { root -> root.toScreenContent() },
        )

        // Assert
        assertEquals(expected = expected, actual = actual)
        assertEquals(
            expected = listOf(TITLE_PARAMETER, SUBTITLE_PARAMETER),
            actual = actual.components.first().values.keys.toList(),
        )
    }

    @Test
    fun `given LocalConsumer provided, when read in composition, then it exposes device, app and catalogVersion`() {
        // Arrange
        val expectedDevice = initialDevice()
        val expectedApp = AppIdentity(version = APP_VERSION, variant = APP_VARIANT, flavor = APP_FLAVOR)
        val expectedCatalogVersion = CatalogVersion(value = FIRST_CATALOG_VERSION)
        val consumer = Consumer(
            device = expectedDevice,
            app = expectedApp,
            catalogVersion = expectedCatalogVersion,
        )

        // Act
        var actual: Consumer? = null
        composeOnce(
            content = {
                CompositionLocalProvider(LocalConsumer provides consumer) {
                    actual = LocalConsumer.current
                }
            },
            observe = {},
        )

        // Assert
        val readConsumer = assertNotNull(actual)
        assertEquals(expected = expectedDevice, actual = readConsumer.device)
        assertEquals(expected = expectedApp, actual = readConsumer.app)
        assertEquals(expected = expectedCatalogVersion, actual = readConsumer.catalogVersion)
    }

    @Test
    fun `given no LocalConsumer provided, when LocalConsumer is read, then it fails naming the producer screen`() {
        // Arrange
        val expectedMessage = "LocalConsumer was read outside a producer screen"

        // Act
        val failure = assertFailsWith<IllegalStateException> {
            composeOnce(
                content = { LocalConsumer.current },
                observe = {},
            )
        }

        // Assert
        assertTrue(actual = expectedMessage in failure.message.orEmpty(), message = failure.message)
    }

    @Test
    fun `given a screen reading LocalConsumer, when device changes, then only device readers recompose`() =
        runTest {
            // Arrange
            val consumer = Consumer(
                device = initialDevice(),
                app = AppIdentity(version = APP_VERSION, variant = APP_VARIANT, flavor = APP_FLAVOR),
                catalogVersion = CatalogVersion(value = FIRST_CATALOG_VERSION),
            )
            var latestTicket: Any? = null
            var appReaderCompositions = 0
            var deviceReaderCompositions = 0
            val dispatcher = StandardTestDispatcher(scheduler = testScheduler)
            val screen = LiveScreen(
                content = {
                    CompositionLocalProvider(LocalConsumer provides consumer) {
                        val held = remember { Any() }
                        SideEffect { latestTicket = held }
                        AppIdentityBanner(onComposed = { appReaderCompositions++ })
                        DeviceThemeBanner(onComposed = { deviceReaderCompositions++ })
                    }
                },
                dispatcher = dispatcher,
                notifier = SnapshotNotifier(dispatcher = dispatcher),
            )

            screen.contents().test {
                // Act
                val first = awaitItem()
                val ticketOnFirstComposition = latestTicket
                consumer.device = updatedDevice()
                val second = awaitItem()

                // Assert
                assertEquals(expected = expectedBannerScreen(theme = Theme.Light), actual = first)
                assertEquals(expected = expectedBannerScreen(theme = Theme.Dark), actual = second)
                assertNotNull(ticketOnFirstComposition)
                assertSame(expected = ticketOnFirstComposition, actual = latestTicket)
                assertEquals(expected = ONE_COMPOSITION, actual = appReaderCompositions)
                assertEquals(expected = TWO_COMPOSITIONS, actual = deviceReaderCompositions)
            }
        }

    private fun <T> composeOnce(content: @Composable () -> Unit, observe: (PlacedNode.Root) -> T): T {
        val root = PlacedNode.Root()
        val recomposer = Recomposer(effectCoroutineContext = EmptyCoroutineContext)
        val composition = Composition(applier = ScreenApplier(root = root), parent = recomposer)
        try {
            composition.setContent(content = content)
            return observe(root)
        } finally {
            composition.dispose()
            recomposer.cancel()
        }
    }

    @Composable
    private fun AppIdentityBanner(onComposed: () -> Unit) {
        SideEffect { onComposed() }
        TestComponent(name = FIRST_COMPONENT, values = listOf(TITLE_PARAMETER to LocalConsumer.current.app.version))
    }

    @Composable
    private fun DeviceThemeBanner(onComposed: () -> Unit) {
        SideEffect { onComposed() }
        TestComponent(
            name = SECOND_COMPONENT,
            values = listOf(TITLE_PARAMETER to LocalConsumer.current.device.theme.name),
        )
    }

    @Composable
    private fun MultiComponentScreen() {
        TestComponent(
            name = FIRST_COMPONENT,
            values = listOf(TITLE_PARAMETER to FIRST_TITLE, SUBTITLE_PARAMETER to FIRST_SUBTITLE),
            slots = listOf(FOOTER_SLOT to null),
        )
        TestComponent(
            catalogVersion = SECOND_CATALOG_VERSION,
            name = SECOND_COMPONENT,
            values = listOf(TITLE_PARAMETER to SECOND_TITLE, SUBTITLE_PARAMETER to null),
            slots = listOf(HEADER_SLOT to { UntitledHeader() }, FOOTER_SLOT to null),
        )
    }

    @Composable
    private fun DoneFooter() {
        TestComponent(name = SECOND_COMPONENT, values = listOf(TITLE_PARAMETER to SECOND_TITLE))
    }

    @Composable
    private fun UntitledHeader() {
        TestComponent(
            name = FIRST_COMPONENT,
            values = listOf(TITLE_PARAMETER to FIRST_TITLE, SUBTITLE_PARAMETER to null),
        )
    }

    @Composable
    private fun TestComponent(
        name: String,
        catalogVersion: String = FIRST_CATALOG_VERSION,
        values: List<Pair<String, String?>> = emptyList(),
        slots: List<Pair<String, (@Composable () -> Unit)?>> = emptyList(),
    ) {
        ConsumerComponent(
            catalogVersion = catalogVersion,
            name = name,
            valueNames = values.map { it.first }.toTypedArray(),
            values = values.map { it.second }.toTypedArray(),
            actionNames = emptyArray(),
            actions = emptyArray(),
            slotNames = slots.map { it.first }.toTypedArray(),
            slots = slots.map { it.second }.toTypedArray(),
        )
    }

    private fun expectedMultiComponentScreen(): ScreenContent =
        ScreenContent(
            components = listOf(
                PlacedComponent(
                    name = FIRST_COMPONENT,
                    values = mapOf(
                        TITLE_PARAMETER to DataValue.Text(value = FIRST_TITLE),
                        SUBTITLE_PARAMETER to DataValue.Text(value = FIRST_SUBTITLE),
                    ),
                ),
                PlacedComponent(
                    name = SECOND_COMPONENT,
                    values = mapOf(TITLE_PARAMETER to DataValue.Text(value = SECOND_TITLE)),
                    slots = mapOf(
                        HEADER_SLOT to listOf(
                            PlacedComponent(
                                name = FIRST_COMPONENT,
                                values = mapOf(TITLE_PARAMETER to DataValue.Text(value = FIRST_TITLE)),
                            ),
                        ),
                    ),
                ),
            ),
        )

    private fun expectedBannerScreen(theme: Theme): ScreenContent =
        ScreenContent(
            components = listOf(
                PlacedComponent(
                    name = FIRST_COMPONENT,
                    values = mapOf(TITLE_PARAMETER to DataValue.Text(value = APP_VERSION)),
                ),
                PlacedComponent(
                    name = SECOND_COMPONENT,
                    values = mapOf(TITLE_PARAMETER to DataValue.Text(value = theme.name)),
                ),
            ),
        )

    private fun initialDevice(): DeviceContext =
        DeviceContext(
            density = INITIAL_DENSITY,
            screenWidthPx = INITIAL_WIDTH_PX,
            screenHeightPx = INITIAL_HEIGHT_PX,
            theme = Theme.Light,
            language = INITIAL_LANGUAGE,
        )

    private fun updatedDevice(): DeviceContext =
        DeviceContext(
            density = INITIAL_DENSITY,
            screenWidthPx = INITIAL_HEIGHT_PX,
            screenHeightPx = INITIAL_WIDTH_PX,
            theme = Theme.Dark,
            language = INITIAL_LANGUAGE,
        )

    private companion object {
        const val FIRST_CATALOG_VERSION = "1"
        const val SECOND_CATALOG_VERSION = "2"
        const val FIRST_COMPONENT = "Header"
        const val SECOND_COMPONENT = "Footer"
        const val TITLE_PARAMETER = "title"
        const val SUBTITLE_PARAMETER = "subtitle"
        const val FIRST_TITLE = "Welcome"
        const val FIRST_SUBTITLE = "Details"
        const val SECOND_TITLE = "Done"
        const val HEADER_SLOT = "header"
        const val FOOTER_SLOT = "footer"
        const val INITIAL_DENSITY = 2.0f
        const val INITIAL_WIDTH_PX = 1080
        const val INITIAL_HEIGHT_PX = 2400
        const val INITIAL_LANGUAGE = "en-US"
        const val APP_VERSION = "1.0.0"
        const val APP_VARIANT = "release"
        const val APP_FLAVOR = "play"
        const val ONE_COMPOSITION = 1
        const val TWO_COMPOSITIONS = 2
    }
}
