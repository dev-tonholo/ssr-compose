package dev.tonholo.ssrcompose.core.document

import dev.tonholo.ssrcompose.core.document.DocumentFrames.box
import dev.tonholo.ssrcompose.core.document.DocumentFrames.component
import dev.tonholo.ssrcompose.core.document.DocumentFrames.localClick
import dev.tonholo.ssrcompose.core.document.DocumentFrames.roundedDecoration
import dev.tonholo.ssrcompose.core.document.DocumentFrames.screen
import dev.tonholo.ssrcompose.core.document.DocumentFrames.screenWithRoot
import dev.tonholo.ssrcompose.core.document.DocumentFrames.scroll
import dev.tonholo.ssrcompose.core.document.DocumentFrames.slot
import dev.tonholo.ssrcompose.core.document.DocumentFrames.style
import dev.tonholo.ssrcompose.core.document.DocumentFrames.switcher
import dev.tonholo.ssrcompose.core.document.DocumentFrames.text
import dev.tonholo.ssrcompose.core.document.DocumentFrames.with
import dev.tonholo.ssrcompose.core.message.MessageDecodingException
import dev.tonholo.ssrcompose.core.message.internal.DocumentScreenCodec
import kotlin.reflect.KClass
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentRulesTest {
    private fun failuresOf(frames: List<ByteArray>): List<KClass<out Throwable>?> = frames.map { frame ->
        runCatching { DocumentScreenCodec.decode(frame = frame) }.exceptionOrNull()?.let { failure -> failure::class }
    }

    private fun allRefused(frames: List<ByteArray>) = frames.map { MessageDecodingException::class }

    private fun variantsOf(count: Int): List<List<JsonObject>> =
        (0 until count).map { index -> listOf(text(id = FIRST_VARIANT_ID + index)) }

    @Test
    fun `given a negative size in any size field, when decoded, then only a decoding error is thrown`() {
        // Arrange
        val emptySlot = slot(nodes = emptyList())
        val frames = listOf(
            screen(text(id = 2).with(key = "width", value = NEGATIVE)),
            screen(text(id = 2).with(key = "height", value = NEGATIVE)),
            screen(scroll(id = 2).with(key = "contentWidth", value = NEGATIVE)),
            screen(scroll(id = 2).with(key = "contentHeight", value = NEGATIVE)),
            screen(component(id = 2).with(key = "maxWidth", value = NEGATIVE)),
            screen(component(id = 2).with(key = "maxHeight", value = NEGATIVE)),
            screen(component(id = 2, slots = mapOf("content" to emptySlot.with(key = "width", value = NEGATIVE)))),
            screen(component(id = 2, slots = mapOf("content" to emptySlot.with(key = "height", value = NEGATIVE)))),
            screen(box(id = 2).with(key = "decoration", value = roundedDecoration(radius = NEGATIVE, borderWidth = 1))),
            screen(box(id = 2).with(key = "decoration", value = roundedDecoration(radius = 1, borderWidth = NEGATIVE))),
        )

        // Act
        val actual = failuresOf(frames = frames)

        // Assert
        assertEquals(expected = allRefused(frames = frames), actual = actual)
    }

    @Test
    fun `given a non-finite size, when a node is built, then it is rejected`() {
        // Arrange
        val infinite = Float.POSITIVE_INFINITY

        // Act
        val actual = runCatching { textNode(width = infinite, x = 0f) }.exceptionOrNull()?.let { it::class }

        // Assert
        assertEquals(expected = IllegalArgumentException::class, actual = actual)
    }

    @Test
    fun `given a non-finite position, when a node is built, then it is rejected`() {
        // Arrange
        val notANumber = Float.NaN

        // Act
        val actual = runCatching { textNode(width = 1f, x = notANumber) }.exceptionOrNull()?.let { it::class }

        // Assert
        assertEquals(expected = IllegalArgumentException::class, actual = actual)
    }

    @Test
    fun `given a non-finite largest width or height, when a switcher is built, then it is rejected`() {
        // Arrange
        val infinite = Float.POSITIVE_INFINITY
        val sizes = listOf(infinite to 1f, 1f to infinite)

        // Act
        val actual = sizes.map { (largestWidth, largestHeight) ->
            runCatching { switcherNode(largestWidth = largestWidth, largestHeight = largestHeight) }
                .exceptionOrNull()?.let { it::class }
        }

        // Assert
        assertEquals(expected = sizes.map { IllegalArgumentException::class }, actual = actual)
    }

    @Test
    fun `given a position beyond 100,000 dp either way, when decoded, then only a decoding error is thrown`() {
        // Arrange
        val frames = listOf(
            screen(text(id = 2).with(key = "x", value = MAX_POSITION + STEP)),
            screen(text(id = 2).with(key = "x", value = -MAX_POSITION - STEP)),
            screen(text(id = 2).with(key = "y", value = MAX_POSITION + STEP)),
            screen(text(id = 2).with(key = "y", value = -MAX_POSITION - STEP)),
        )

        // Act
        val actual = failuresOf(frames = frames)

        // Assert
        assertEquals(expected = allRefused(frames = frames), actual = actual)
    }

    @Test
    fun `given a font size not above 0 or above 1,000, when decoded, then only a decoding error is thrown`() {
        // Arrange
        val frames = listOf(0f, -STEP, MAX_FONT_SIZE + STEP).map { size ->
            screen(text(id = 2).with(key = "style", value = style().with(key = "size", value = size)))
        }

        // Act
        val actual = failuresOf(frames = frames)

        // Assert
        assertEquals(expected = allRefused(frames = frames), actual = actual)
    }

    @Test
    fun `given a line height not above 0 or above 1,000, when decoded, then only a decoding error is thrown`() {
        // Arrange
        val frames = listOf(0f, -STEP, MAX_FONT_SIZE + STEP).map { lineHeight ->
            screen(text(id = 2).with(key = "style", value = style().with(key = "lineHeight", value = lineHeight)))
        }

        // Act
        val actual = failuresOf(frames = frames)

        // Assert
        assertEquals(expected = allRefused(frames = frames), actual = actual)
    }

    @Test
    fun `given maxLines below 1, when decoded, then only a decoding error is thrown`() {
        // Arrange
        val frames = listOf(0, -1).map { maxLines -> screen(text(id = 2).with(key = "maxLines", value = maxLines)) }

        // Act
        val actual = failuresOf(frames = frames)

        // Assert
        assertEquals(expected = allRefused(frames = frames), actual = actual)
    }

    @Test
    fun `given a weight outside 1 to 1,000, when decoded, then only a decoding error is thrown`() {
        // Arrange
        val frames = listOf(MIN_WEIGHT - 1, MAX_WEIGHT + 1).map { weight ->
            screen(text(id = 2).with(key = "style", value = style().with(key = "weight", value = weight)))
        }

        // Act
        val actual = failuresOf(frames = frames)

        // Assert
        assertEquals(expected = allRefused(frames = frames), actual = actual)
    }

    @Test
    fun `given an id repeated anywhere in the document, when decoded, then only a decoding error is thrown`() {
        // Arrange
        val frames = listOf(
            screen(text(id = 2), text(id = 2)),
            screen(text(id = DocumentFrames.ROOT_ID)),
            screen(box(id = 2, children = listOf(text(id = 3))), text(id = 3)),
            screen(scroll(id = 2, children = listOf(text(id = 3))), text(id = 3)),
            screen(switcher(id = 2, variants = listOf(listOf(text(id = 3)), listOf(text(id = 4)))), text(id = 4)),
            screen(component(id = 2, slots = mapOf("content" to slot(nodes = listOf(text(id = 3))))), text(id = 3)),
        )

        // Act
        val actual = failuresOf(frames = frames)

        // Assert
        assertEquals(expected = allRefused(frames = frames), actual = actual)
    }

    @Test
    fun `given a local click to a switcher or variant the document lacks, when decoded, then it is refused`() {
        // Arrange
        val switcher = switcher(id = 2, variants = variantsOf(count = 2))
        val frames = listOf(
            screen(switcher, box(id = 9).with(key = "click", value = localClick(switcher = 8, variant = 0))),
            screen(switcher, text(id = 9).with(key = "click", value = localClick(switcher = 9, variant = 0))),
            screen(switcher, box(id = 9).with(key = "click", value = localClick(switcher = 2, variant = 2))),
            screen(switcher, box(id = 9).with(key = "click", value = localClick(switcher = 2, variant = -1))),
        )

        // Act
        val actual = failuresOf(frames = frames)

        // Assert
        assertEquals(expected = allRefused(frames = frames), actual = actual)
    }

    @Test
    fun `given a selected variant the switcher does not have, when decoded, then only a decoding error is thrown`() {
        // Arrange
        val frames = listOf(2, -1).map { selected ->
            screen(switcher(id = 2, variants = variantsOf(count = 2)).with(key = "selected", value = selected))
        }

        // Act
        val actual = failuresOf(frames = frames)

        // Assert
        assertEquals(expected = allRefused(frames = frames), actual = actual)
    }

    @Test
    fun `given a largest size smaller than a variant's box, when decoded, then only a decoding error is thrown`() {
        // Arrange
        val wideVariant = listOf(text(id = 4).with(key = "x", value = 1))
        val tallVariant = listOf(text(id = 4).with(key = "y", value = 1))
        val frames = listOf(
            screen(switcher(id = 2, variants = variantsOf(count = 2)).with(key = "width", value = NODE_SIZE + 1)),
            screen(switcher(id = 2, variants = variantsOf(count = 2)).with(key = "height", value = NODE_SIZE + 1)),
            screen(switcher(id = 2, variants = listOf(listOf(text(id = 3)), wideVariant))),
            screen(switcher(id = 2, variants = listOf(listOf(text(id = 3)), tallVariant))),
        )

        // Act
        val actual = failuresOf(frames = frames)

        // Assert
        assertEquals(expected = allRefused(frames = frames), actual = actual)
    }

    @Test
    fun `given a switcher with fewer than 2 or more than 16 variants, when decoded, then it is refused`() {
        // Arrange
        val frames = listOf(MIN_VARIANTS - 1, MAX_VARIANTS + 1).map { count ->
            screen(switcher(id = 2, variants = variantsOf(count = count)))
        }

        // Act
        val actual = failuresOf(frames = frames)

        // Assert
        assertEquals(expected = allRefused(frames = frames), actual = actual)
    }

    @Test
    fun `given a root that is not a Box, when decoded, then only a decoding error is thrown`() {
        // Arrange
        val frames = listOf(screenWithRoot(root = text(id = DocumentFrames.ROOT_ID)))

        // Act
        val actual = failuresOf(frames = frames)

        // Assert
        assertEquals(expected = allRefused(frames = frames), actual = actual)
    }

    @Test
    fun `given a color not written #AARRGGBB, when decoded, then only a decoding error is thrown`() {
        // Arrange
        val colors = listOf("#FF", "FF000000", "FF00000000", "#GG000000", "#FF00000", "#FF0000000", "#+F000000")
        val frames = colors.map { color ->
            screen(text(id = 2).with(key = "style", value = style().with(key = "color", value = color)))
        }

        // Act
        val actual = failuresOf(frames = frames)

        // Assert
        assertEquals(expected = allRefused(frames = frames), actual = actual)
    }

    @Test
    fun `given a document at the edge of every limit, when decoded, then it is read`() {
        // Arrange
        val frames = listOf(
            screen(text(id = 2).with(key = "x", value = -MAX_POSITION).with(key = "y", value = MAX_POSITION)),
            screen(text(id = 2).with(key = "x", value = MAX_POSITION).with(key = "y", value = -MAX_POSITION)),
            screen(text(id = 2).with(key = "width", value = 0).with(key = "height", value = 0)),
            screen(text(id = 2).with(key = "style", value = style().with(key = "size", value = MAX_FONT_SIZE))),
            screen(text(id = 2).with(key = "style", value = style().with(key = "lineHeight", value = MAX_FONT_SIZE))),
            screen(text(id = 2).with(key = "style", value = style().with(key = "weight", value = MIN_WEIGHT))),
            screen(text(id = 2).with(key = "style", value = style().with(key = "weight", value = MAX_WEIGHT))),
            screen(text(id = 2).with(key = "style", value = style().with(key = "color", value = "#ff6200ee"))),
            screen(text(id = 2).with(key = "maxLines", value = 1)),
            screen(switcher(id = 2, variants = variantsOf(count = MIN_VARIANTS))),
            screen(
                switcher(id = 2, variants = variantsOf(count = MAX_VARIANTS))
                    .with(key = "selected", value = LAST_OF_MAX),
            ),
            screen(
                switcher(id = 2, variants = variantsOf(count = MIN_VARIANTS)),
                box(id = 9).with(key = "click", value = localClick(switcher = 2, variant = 1)),
            ),
        )

        // Act
        val actual = failuresOf(frames = frames)

        // Assert
        assertEquals(expected = frames.map { null }, actual = actual)
    }

    private fun textNode(width: Float, x: Float) = DocumentNode.Text(
        id = 2,
        x = x,
        y = 0f,
        width = width,
        height = 1f,
        text = "Hi",
        style = TextStyleSpec(size = 14f, weight = 400, color = DocumentColor(argb = 0xFF000000u)),
    )

    private fun switcherNode(largestWidth: Float, largestHeight: Float) = DocumentNode.Switcher(
        id = 3,
        x = 0f,
        y = 0f,
        width = 1f,
        height = 1f,
        selected = 0,
        largestWidth = largestWidth,
        largestHeight = largestHeight,
        variants = listOf(listOf(textNode(width = 1f, x = 0f)), listOf(textNode(width = 1f, x = 0f))),
    )

    private companion object {
        const val NEGATIVE = -0.5f
        const val STEP = 0.5f
        const val NODE_SIZE = DocumentFrames.NODE_SIZE
        const val FIRST_VARIANT_ID = 100
        const val MAX_POSITION = 100_000f
        const val MAX_FONT_SIZE = 1_000f
        const val MIN_WEIGHT = 1
        const val MAX_WEIGHT = 1_000
        const val MIN_VARIANTS = 2
        const val MAX_VARIANTS = 16
        const val LAST_OF_MAX = MAX_VARIANTS - 1
    }
}
