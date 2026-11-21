package dev.tonholo.ssrcompose.producer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import app.cash.turbine.test
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.handshake.AppIdentity
import dev.tonholo.ssrcompose.core.handshake.DeviceContext
import dev.tonholo.ssrcompose.core.handshake.FontScaleCurve
import dev.tonholo.ssrcompose.core.handshake.FontScalePoint
import dev.tonholo.ssrcompose.core.handshake.LayoutDirection
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
            listOf(TITLE_PARAMETER to DataValue.Text(value = FIRST_TITLE), SUBTITLE_PARAMETER to FIRST_SUBTITLE_VALUE),
            listOf(TITLE_PARAMETER to DataValue.Text(value = SECOND_TITLE)),
        )

        // Act
        val components = composeOnce(
            content = {
                TestComponent(
                    name = FIRST_COMPONENT,
                    values = mapOf(TITLE_PARAMETER to FIRST_TITLE_VALUE, SUBTITLE_PARAMETER to FIRST_SUBTITLE_VALUE),
                )
                TestComponent(name = SECOND_COMPONENT, values = mapOf(TITLE_PARAMETER to SECOND_TITLE_VALUE))
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
                    values = mapOf(TITLE_PARAMETER to FIRST_TITLE_VALUE),
                )
                TestComponent(
                    catalogVersion = SECOND_CATALOG_VERSION,
                    name = SECOND_COMPONENT,
                    values = mapOf(TITLE_PARAMETER to SECOND_TITLE_VALUE),
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
        val expectedValues = mapOf(TITLE_PARAMETER to FIRST_TITLE_VALUE)

        // Act
        val component = composeOnce(
            content = {
                TestComponent(
                    name = FIRST_COMPONENT,
                    values = mapOf(TITLE_PARAMETER to FIRST_TITLE_VALUE, SUBTITLE_PARAMETER to null),
                )
            },
            observe = { root -> root.children.filterIsInstance<PlacedNode.Component>().single() },
        )

        // Assert
        assertEquals(expected = expectedValues, actual = component.values)
    }

    @Test
    fun `given a required nullable value given null, when converted to screen content, then it is sent as None`() {
        // Arrange
        val expected = ScreenContent(
            components = listOf(
                PlacedComponent(
                    name = FIRST_COMPONENT,
                    values = mapOf(TITLE_PARAMETER to FIRST_TITLE_VALUE, SUBTITLE_PARAMETER to DataValue.None),
                ),
            ),
        )

        // Act
        val actual = composeOnce(
            content = {
                TestComponent(
                    name = FIRST_COMPONENT,
                    values = mapOf(TITLE_PARAMETER to FIRST_TITLE_VALUE, SUBTITLE_PARAMETER to DataValue.None),
                )
            },
            observe = { root -> root.toScreenContent() },
        )

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given an action parameter that was not given, when composed, then that action is absent`() {
        // Arrange
        val expectedActions = listOf(SELECT_ACTION)

        // Act
        val component = composeOnce(
            content = {
                TestComponent(
                    name = FIRST_COMPONENT,
                    actions = mapOf(SELECT_ACTION to RemoteAction1 {}, CLOSE_ACTION to null),
                )
            },
            observe = { root -> root.children.filterIsInstance<PlacedNode.Component>().single() },
        )

        // Assert
        assertEquals(expected = expectedActions, actual = component.actions.keys.toList())
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
                    slots = mapOf(HEADER_SLOT to { DoneFooter() }, FOOTER_SLOT to null),
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
        val expectedCatalogVersion = FIRST_CATALOG_VERSION
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
                catalogVersion = FIRST_CATALOG_VERSION,
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

    @Test
    fun `given an optional slot that becomes given, when recomposed, then each slot keeps its own name and content`() =
        runTest {
            // Arrange
            val expected = ScreenContent(
                components = listOf(
                    PlacedComponent(
                        name = FIRST_COMPONENT,
                        slots = mapOf(
                            HEADER_SLOT to listOf(UNTITLED_HEADER),
                            FOOTER_SLOT to listOf(DONE_FOOTER),
                        ),
                    ),
                ),
            )
            val headerGiven = mutableStateOf(false)
            val dispatcher = StandardTestDispatcher(scheduler = testScheduler)
            val screen = LiveScreen(
                content = { SlotsWithOptionalHeader(headerGiven = headerGiven.value) },
                dispatcher = dispatcher,
                notifier = SnapshotNotifier(dispatcher = dispatcher),
            )

            screen.contents().test {
                awaitItem()

                // Act
                headerGiven.value = true
                val actual = awaitItem()

                // Assert
                assertEquals(expected = expected, actual = actual)
            }
        }

    @Test
    fun `given a given slot that becomes omitted, when recomposed, then the other slot keeps its name and content`() =
        runTest {
            // Arrange
            val expected = ScreenContent(
                components = listOf(
                    PlacedComponent(
                        name = FIRST_COMPONENT,
                        slots = mapOf(
                            FOOTER_SLOT to listOf(DONE_FOOTER),
                        ),
                    ),
                ),
            )
            val headerGiven = mutableStateOf(true)
            val dispatcher = StandardTestDispatcher(scheduler = testScheduler)
            val screen = LiveScreen(
                content = { SlotsWithOptionalHeader(headerGiven = headerGiven.value) },
                dispatcher = dispatcher,
                notifier = SnapshotNotifier(dispatcher = dispatcher),
            )

            screen.contents().test {
                awaitItem()

                // Act
                headerGiven.value = false
                val actual = awaitItem()

                // Assert
                assertEquals(expected = expected, actual = actual)
            }
        }

    @Composable
    private fun SlotsWithOptionalHeader(headerGiven: Boolean) {
        val header: (@Composable () -> Unit)? = if (headerGiven) untitledHeaderSlot else null
        TestComponent(
            name = FIRST_COMPONENT,
            slots = mapOf(HEADER_SLOT to header, FOOTER_SLOT to doneFooterSlot),
        )
    }

    private val untitledHeaderSlot: @Composable () -> Unit = { UntitledHeader() }
    private val doneFooterSlot: @Composable () -> Unit = { DoneFooter() }

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
        TestComponent(
            name = FIRST_COMPONENT,
            values = mapOf(TITLE_PARAMETER to DataValue.Text(value = LocalConsumer.current.app.version)),
        )
    }

    @Composable
    private fun DeviceThemeBanner(onComposed: () -> Unit) {
        SideEffect { onComposed() }
        TestComponent(
            name = SECOND_COMPONENT,
            values = mapOf(TITLE_PARAMETER to DataValue.Text(value = LocalConsumer.current.device.theme.name)),
        )
    }

    @Composable
    private fun MultiComponentScreen() {
        TestComponent(
            name = FIRST_COMPONENT,
            values = mapOf(TITLE_PARAMETER to FIRST_TITLE_VALUE, SUBTITLE_PARAMETER to FIRST_SUBTITLE_VALUE),
            slots = mapOf(FOOTER_SLOT to null),
        )
        TestComponent(
            catalogVersion = SECOND_CATALOG_VERSION,
            name = SECOND_COMPONENT,
            values = mapOf(TITLE_PARAMETER to SECOND_TITLE_VALUE, SUBTITLE_PARAMETER to null),
            slots = mapOf(HEADER_SLOT to { UntitledHeader() }, FOOTER_SLOT to null),
        )
    }

    @Composable
    private fun DoneFooter() {
        TestComponent(name = SECOND_COMPONENT, values = mapOf(TITLE_PARAMETER to SECOND_TITLE_VALUE))
    }

    @Composable
    private fun UntitledHeader() {
        TestComponent(
            name = FIRST_COMPONENT,
            values = mapOf(TITLE_PARAMETER to FIRST_TITLE_VALUE, SUBTITLE_PARAMETER to null),
        )
    }

    @Composable
    private fun TestComponent(
        name: String,
        catalogVersion: CatalogVersion = FIRST_CATALOG_VERSION,
        values: Map<String, DataValue?> = emptyMap(),
        actions: Map<String, RemoteAction1<String>?> = emptyMap(),
        slots: Map<String, (@Composable () -> Unit)?> = emptyMap(),
    ) {
        ConsumerComponent(
            catalogVersion = catalogVersion,
            name = name,
            values = values,
            actions = actions,
            slots = slots,
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

    private fun linearCurve(): FontScaleCurve =
        FontScaleCurve(
            points = FontScaleCurve.SAMPLED_SIZES.map { sp -> FontScalePoint(sp = sp, dp = sp * INITIAL_FONT_SCALE) },
        )

    private fun initialDevice(): DeviceContext =
        DeviceContext(
            density = INITIAL_DENSITY,
            screenWidthPx = INITIAL_WIDTH_PX,
            screenHeightPx = INITIAL_HEIGHT_PX,
            theme = Theme.Light,
            language = INITIAL_LANGUAGE,
            fontScale = INITIAL_FONT_SCALE,
            fontScaleCurve = linearCurve(),
            layoutDirection = LayoutDirection.Ltr,
        )

    private fun updatedDevice(): DeviceContext =
        DeviceContext(
            density = INITIAL_DENSITY,
            screenWidthPx = INITIAL_HEIGHT_PX,
            screenHeightPx = INITIAL_WIDTH_PX,
            theme = Theme.Dark,
            language = INITIAL_LANGUAGE,
            fontScale = INITIAL_FONT_SCALE,
            fontScaleCurve = linearCurve(),
            layoutDirection = LayoutDirection.Ltr,
        )

    private companion object {
        const val INITIAL_FONT_SCALE = 1f
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
        const val SELECT_ACTION = "onSelect"
        const val CLOSE_ACTION = "onClose"
        val FIRST_CATALOG_VERSION = CatalogVersion(value = "1")
        val SECOND_CATALOG_VERSION = CatalogVersion(value = "2")
        val FIRST_TITLE_VALUE = DataValue.Text(value = FIRST_TITLE)
        val FIRST_SUBTITLE_VALUE = DataValue.Text(value = FIRST_SUBTITLE)
        val SECOND_TITLE_VALUE = DataValue.Text(value = SECOND_TITLE)
        val UNTITLED_HEADER = PlacedComponent(
            name = FIRST_COMPONENT,
            values = mapOf(TITLE_PARAMETER to FIRST_TITLE_VALUE),
        )
        val DONE_FOOTER = PlacedComponent(
            name = SECOND_COMPONENT,
            values = mapOf(TITLE_PARAMETER to SECOND_TITLE_VALUE),
        )
    }
}
