package dev.tonholo.ssrcompose.server.screen

import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.FragmentNode
import dev.tonholo.ssrcompose.protocol.GreetingCard
import dev.tonholo.ssrcompose.protocol.GreetingCardProps
import dev.tonholo.ssrcompose.protocol.ReferenceNode
import dev.tonholo.ssrcompose.server.tree.renderTree
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class HomeScreenTest {
    private fun clockAt(time: String): Clock =
        Clock.fixed(Instant.parse("2026-01-01T${time}:00Z"), ZoneOffset.UTC)

    private fun cardTitle(reference: ReferenceNode): String =
        Json.decodeFromJsonElement(GreetingCard.propsSerializer, reference.props).title

    @Test
    fun `given a clock at 09 00, when the home screen is rendered, then the tree is a morning card then a fragment`() =
        runTest {
            // Arrange
            val clock = clockAt("09:00")

            // Act
            val tree = renderTree { HomeScreen(clock = clock) }

            // Assert
            val column = assertIs<ColumnNode>(tree)
            assertEquals(expected = 2, actual = column.children.size)
            val card = assertIs<ReferenceNode>(column.children[0])
            assertEquals(expected = GreetingCard.id, actual = card.componentId)
            assertEquals(expected = "Good morning", actual = cardTitle(card))
            assertIs<FragmentNode>(column.children[1])
        }

    @Test
    fun `given a clock at 20 00, when the home screen is rendered, then the card title is Good evening`() =
        runTest {
            // Arrange
            val clock = clockAt("20:00")

            // Act
            val tree = renderTree { HomeScreen(clock = clock) }

            // Assert
            val card = assertIs<ReferenceNode>(assertIs<ColumnNode>(tree).children[0])
            assertEquals(expected = "Good evening", actual = cardTitle(card))
        }

    @Test
    fun `given a clock at 09 00, when the home screen is rendered, then the card carries the fixed message`() =
        runTest {
            // Arrange
            val clock = clockAt("09:00")

            // Act
            val tree = renderTree { HomeScreen(clock = clock) }

            // Assert
            val card = assertIs<ReferenceNode>(assertIs<ColumnNode>(tree).children[0])
            val props = Json.decodeFromJsonElement(GreetingCard.propsSerializer, card.props)
            val expected = GreetingCardProps(title = "Good morning", message = HOME_CARD_MESSAGE)
            assertEquals(expected = expected, actual = props)
        }
}
