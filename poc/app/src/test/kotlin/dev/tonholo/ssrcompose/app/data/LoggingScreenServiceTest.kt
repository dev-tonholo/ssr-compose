package dev.tonholo.ssrcompose.app.data

import app.cash.turbine.test
import dev.tonholo.ssrcompose.app.presentation.FakeScreenService
import dev.tonholo.ssrcompose.protocol.ActionEvent
import dev.tonholo.ssrcompose.protocol.ActionId
import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.FragmentNode
import dev.tonholo.ssrcompose.protocol.ReferenceNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class LoggingScreenServiceTest {
    private val tree = ColumnNode(
        children = listOf(
            ReferenceNode(componentId = "greeting-card", props = JsonObject(mapOf("title" to JsonPrimitive("Hi")))),
            FragmentNode(document = byteArrayOf(1, 2, 3)),
        ),
    )

    @Test
    fun `given a session that emits a tree, when collected, then the open line and the tree are logged`() = runTest {
        // Arrange
        val lines = mutableListOf<String>()
        val delegate = FakeScreenService()
        val service = LoggingScreenService(delegate = delegate, log = { line -> lines += line })

        // Act
        service.session(name = "counter", events = emptyFlow()).test {
            delegate.emit(tree)
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        // Assert
        val expected = listOf(
            "session counter opened",
            """<- column[greeting-card{"title":"Hi"}, fragment(3 bytes)]""",
        )
        assertEquals(expected = expected, actual = lines.take(expected.size))
    }

    @Test
    fun `given an open session, when the app sends an action, then the action id is logged`() = runTest {
        // Arrange
        val lines = mutableListOf<String>()
        val delegate = FakeScreenService()
        val service = LoggingScreenService(delegate = delegate, log = { line -> lines += line })
        val events = MutableSharedFlow<ActionEvent>()

        // Act
        service.session(name = "counter", events = events).test {
            runCurrent()
            events.emit(ActionEvent(actionId = ActionId(value = "action-1")))
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        // Assert
        assertEquals(expected = true, actual = "-> action action-1" in lines)
    }

    @Test
    fun `given a session that fails, when collected, then the close line carries the reason`() = runTest {
        // Arrange
        val lines = mutableListOf<String>()
        val delegate = FakeScreenService()
        val service = LoggingScreenService(delegate = delegate, log = { line -> lines += line })

        // Act
        service.session(name = "counter", events = emptyFlow()).test {
            delegate.fail(IllegalStateException("boom"))
            awaitError()
        }

        // Assert
        assertEquals(expected = "session counter closed: boom", actual = lines.last())
    }
}
