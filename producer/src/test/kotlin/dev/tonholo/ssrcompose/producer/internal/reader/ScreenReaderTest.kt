package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.core.document.Decoration
import dev.tonholo.ssrcompose.core.document.DocumentColor
import dev.tonholo.ssrcompose.core.document.DocumentNode
import dev.tonholo.ssrcompose.core.document.NodeSemantics
import dev.tonholo.ssrcompose.core.document.TextStyleSpec
import dev.tonholo.ssrcompose.producer.ScaledBasicText
import dev.tonholo.ssrcompose.producer.internal.font.bundledRobotoFamily
import dev.tonholo.ssrcompose.producer.internal.text.DEFAULT_FONT_SIZE_SP
import dev.tonholo.ssrcompose.producer.testing.DEFAULT_SCENE_HEIGHT_PX
import dev.tonholo.ssrcompose.producer.testing.DEFAULT_SCENE_WIDTH_PX
import dev.tonholo.ssrcompose.producer.testing.READ_DENSITY
import dev.tonholo.ssrcompose.producer.testing.documentOf
import dev.tonholo.ssrcompose.producer.testing.robotoStyle
import dev.tonholo.ssrcompose.producer.testing.withRenderedScene
import dev.tonholo.ssrcompose.producer.testing.withoutIds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val OUTER_PADDING_DP = 8
private const val INNER_PADDING_DP = 6
private const val CONTENT_SIZE_DP = 20
private const val CORNER_DP = 4
private const val BORDER_DP = 1
private const val TEXT_SIZE_SP = 16
private const val TEXT_MAX_LINES = 2
private const val ROW_COUNT = 10
private const val SCROLL_SIZE_DP = 50
private const val SMALL_BOX_DP = 10
private const val SPACER_DP = 4
private const val TEXT = "Hello"
private const val EXPECTED_DEFAULT_FONT_SIZE_SP = 14f
private val RED = DocumentColor(argb = 0xFFFF0000u)
private val BLACK = DocumentColor(argb = 0xFF000000u)
private val WHITE = DocumentColor(argb = 0xFFFFFFFFu)

class ScreenReaderTest {
    @Test
    fun `given a screen that shows nothing, when read, then the root box has the scene's size and no children`() {
        // Arrange
        val expected = DocumentNode.Box(
            id = 0,
            x = 0f,
            y = 0f,
            width = DEFAULT_SCENE_WIDTH_PX / READ_DENSITY,
            height = DEFAULT_SCENE_HEIGHT_PX / READ_DENSITY,
        )

        // Act
        val actual = documentOf(content = {}).root.withoutIds()

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a screen with one node, when read, then the ids are the semantics ids`() {
        // Arrange
        val content: @Composable () -> Unit = { Box(Modifier.size(SMALL_BOX_DP.dp).background(Color.Red)) }

        // Act
        val (expected, actual) = withRenderedScene(content = content) { scene ->
            scene.read { roots ->
                val read = readScreen(roots = roots, density = scene.density, fontScaleCurve = scene.fontScaleCurve)
                val document = assertIs<ScreenRead.Document>(value = read)
                val root = roots.single()
                listOf(root.id, root.children.single().id) to
                    listOf(document.document.root.id, document.document.root.children.single().id)
            }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a box with padding around its background and border, when read, then its box is where they draw`() {
        // Arrange
        val decoratedSide = CONTENT_SIZE_DP + 2f * INNER_PADDING_DP
        val expected = listOf<DocumentNode>(
            DocumentNode.Box(
                id = 0,
                x = OUTER_PADDING_DP.toFloat(),
                y = OUTER_PADDING_DP.toFloat(),
                width = decoratedSide,
                height = decoratedSide,
                decoration = Decoration(
                    background = RED,
                    border = Decoration.Border(width = BORDER_DP.toFloat(), color = BLACK),
                    shape = Decoration.Shape.Rounded(
                        topStart = CORNER_DP.toFloat(),
                        topEnd = CORNER_DP.toFloat(),
                        bottomEnd = CORNER_DP.toFloat(),
                        bottomStart = CORNER_DP.toFloat(),
                    ),
                ),
            ),
        )

        // Act
        val actual = documentOf {
            Box(
                Modifier
                    .padding(OUTER_PADDING_DP.dp)
                    .background(color = Color.Red, shape = RoundedCornerShape(CORNER_DP.dp))
                    .border(width = BORDER_DP.dp, color = Color.Black, shape = RoundedCornerShape(CORNER_DP.dp))
                    .padding(INNER_PADDING_DP.dp)
                    .size(CONTENT_SIZE_DP.dp),
            )
        }.root.children.map { it.withoutIds() }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a padded text in the bundled Roboto, when read, then it is a text node in its laid out box`() {
        // Arrange
        var laidOut = IntSize.Zero
        val style = robotoStyle(sizeSp = TEXT_SIZE_SP, weight = FontWeight.Medium).copy(color = Color.White)

        // Act
        val actual = documentOf {
            ScaledBasicText(
                text = TEXT,
                modifier = Modifier.padding(OUTER_PADDING_DP.dp),
                style = style,
                maxLines = TEXT_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { laidOut = it.size },
            )
        }.root.children.map { it.withoutIds() }

        // Assert
        val expected = listOf<DocumentNode>(
            DocumentNode.Text(
                id = 0,
                x = OUTER_PADDING_DP.toFloat(),
                y = OUTER_PADDING_DP.toFloat(),
                width = laidOut.width / READ_DENSITY,
                height = laidOut.height / READ_DENSITY,
                text = TEXT,
                style = TextStyleSpec(size = TEXT_SIZE_SP.toFloat(), weight = FontWeight.Medium.weight, color = WHITE),
                maxLines = TEXT_MAX_LINES,
                overflow = DocumentNode.Text.Overflow.Ellipsis,
            ),
        )
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a text with no font size or color, when read, then it has Compose's default size and black`() {
        // Arrange
        val expected = TextStyleSpec(
            size = EXPECTED_DEFAULT_FONT_SIZE_SP,
            weight = FontWeight.Normal.weight,
            color = BLACK,
        )

        // Act
        val actual = documentOf {
            ScaledBasicText(text = TEXT, style = TextStyle(fontFamily = bundledRobotoFamily()))
        }.root.children.map { (it as DocumentNode.Text).style }

        // Assert
        assertEquals(expected = listOf(expected), actual = actual)
    }

    @Test
    fun `given a vertical scroll of fixed rows, when read, then it is a scroll node holding all its content`() {
        // Arrange
        val expectedRows = List(ROW_COUNT) { index ->
            DocumentNode.Box(
                id = 0,
                x = 0f,
                y = index * CONTENT_SIZE_DP.toFloat(),
                width = CONTENT_SIZE_DP.toFloat(),
                height = CONTENT_SIZE_DP.toFloat(),
                decoration = Decoration(background = RED),
            )
        }
        val expected = listOf<DocumentNode>(
            DocumentNode.Scroll(
                id = 0,
                x = 0f,
                y = 0f,
                width = SCROLL_SIZE_DP.toFloat(),
                height = SCROLL_SIZE_DP.toFloat(),
                orientation = DocumentNode.Scroll.Orientation.Vertical,
                contentWidth = SCROLL_SIZE_DP.toFloat(),
                contentHeight = ROW_COUNT * CONTENT_SIZE_DP.toFloat(),
                children = expectedRows,
            ),
        )

        // Act
        val actual = documentOf {
            Column(Modifier.size(SCROLL_SIZE_DP.dp).verticalScroll(rememberScrollState())) {
                repeat(ROW_COUNT) { Box(Modifier.size(CONTENT_SIZE_DP.dp).background(Color.Red)) }
            }
        }.root.children.map { it.withoutIds() }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a horizontal scroll of fixed columns, when read, then it is a scroll node holding all its content`() {
        // Arrange
        val expectedColumns = List(ROW_COUNT) { index ->
            DocumentNode.Box(
                id = 0,
                x = index * CONTENT_SIZE_DP.toFloat(),
                y = 0f,
                width = CONTENT_SIZE_DP.toFloat(),
                height = CONTENT_SIZE_DP.toFloat(),
                decoration = Decoration(background = RED),
            )
        }
        val expected = listOf<DocumentNode>(
            DocumentNode.Scroll(
                id = 0,
                x = 0f,
                y = 0f,
                width = SCROLL_SIZE_DP.toFloat(),
                height = SCROLL_SIZE_DP.toFloat(),
                orientation = DocumentNode.Scroll.Orientation.Horizontal,
                contentWidth = ROW_COUNT * CONTENT_SIZE_DP.toFloat(),
                contentHeight = SCROLL_SIZE_DP.toFloat(),
                children = expectedColumns,
            ),
        )

        // Act
        val actual = documentOf {
            Row(Modifier.size(SCROLL_SIZE_DP.dp).horizontalScroll(rememberScrollState())) {
                repeat(ROW_COUNT) { Box(Modifier.size(CONTENT_SIZE_DP.dp).background(Color.Red)) }
            }
        }.root.children.map { it.withoutIds() }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given boxes in a padded column, a row and a padded box, when read, then they are placed in the root box`() {
        // Arrange
        val expected = listOf(
            redBox(x = OUTER_PADDING_DP.toFloat(), y = OUTER_PADDING_DP.toFloat()),
            redBox(x = OUTER_PADDING_DP + SMALL_BOX_DP + SPACER_DP.toFloat(), y = OUTER_PADDING_DP.toFloat()),
            redBox(
                x = OUTER_PADDING_DP + INNER_PADDING_DP.toFloat(),
                y = OUTER_PADDING_DP + SMALL_BOX_DP + INNER_PADDING_DP.toFloat(),
            ),
        )

        // Act
        val actual = documentOf {
            Column(Modifier.padding(OUTER_PADDING_DP.dp)) {
                Row {
                    Box(Modifier.size(SMALL_BOX_DP.dp).background(Color.Red))
                    Spacer(Modifier.size(SPACER_DP.dp))
                    Box(Modifier.size(SMALL_BOX_DP.dp).background(Color.Red))
                }
                Box(Modifier.padding(INNER_PADDING_DP.dp)) {
                    Box(Modifier.size(SMALL_BOX_DP.dp).background(Color.Red))
                }
            }
        }.root.children.map { it.withoutIds() }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a padded clickable box, when read, then it is a box at its click area with no click yet`() {
        // Arrange
        val side = CONTENT_SIZE_DP + 2f * INNER_PADDING_DP
        val expected = listOf<DocumentNode>(
            DocumentNode.Box(
                id = 0,
                x = OUTER_PADDING_DP.toFloat(),
                y = OUTER_PADDING_DP.toFloat(),
                width = side,
                height = side,
            ),
        )

        // Act
        val actual = documentOf {
            Box(
                Modifier
                    .padding(OUTER_PADDING_DP.dp)
                    .clickable {}
                    .padding(INNER_PADDING_DP.dp)
                    .size(CONTENT_SIZE_DP.dp),
            )
        }.root.children.map { it.withoutIds() }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a padded box marked as a heading, when read, then it is sent with its semantics`() {
        // Arrange
        val side = SMALL_BOX_DP + 2f * INNER_PADDING_DP
        val expected = listOf<DocumentNode>(
            DocumentNode.Box(
                id = 0,
                x = OUTER_PADDING_DP.toFloat(),
                y = OUTER_PADDING_DP.toFloat(),
                width = side,
                height = side,
                semantics = NodeSemantics(heading = true),
            ),
        )

        // Act
        val actual = documentOf {
            Box(
                Modifier
                    .padding(OUTER_PADDING_DP.dp)
                    .semantics { heading() }
                    .padding(INNER_PADDING_DP.dp)
                    .size(SMALL_BOX_DP.dp),
            )
        }.root.children.map { it.withoutIds() }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    private fun redBox(x: Float, y: Float) = DocumentNode.Box(
        id = 0,
        x = x,
        y = y,
        width = SMALL_BOX_DP.toFloat(),
        height = SMALL_BOX_DP.toFloat(),
        decoration = Decoration(background = RED),
    )
}
