package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.core.document.Decoration
import dev.tonholo.ssrcompose.core.document.DocumentColor
import dev.tonholo.ssrcompose.core.document.DocumentNode
import dev.tonholo.ssrcompose.core.document.TextStyleSpec
import dev.tonholo.ssrcompose.producer.ScaledBasicText
import dev.tonholo.ssrcompose.producer.testing.SceneSpec
import dev.tonholo.ssrcompose.producer.testing.documentOf
import dev.tonholo.ssrcompose.producer.testing.robotoStyle
import dev.tonholo.ssrcompose.producer.testing.withoutIds
import kotlin.test.Test
import kotlin.test.assertEquals

// R20 example 1's viewport, 411.4 by 843.4 dp, is 2057 by 4217 whole pixels at density 5.
private const val DENSITY = 5f
private const val VIEWPORT_WIDTH_PX = 2057
private const val VIEWPORT_HEIGHT_PX = 4217
private const val VIEWPORT_WIDTH_DP = 411.4f
private const val VIEWPORT_HEIGHT_DP = 843.4f

// The numbers of R20 example 1.
private const val SCREEN_PADDING_DP = 16
private const val CARD_HEIGHT_DP = 96
private const val CARD_CORNER_DP = 12
private const val CARD_WIDTH_DP = 379.4f
private const val CARD_TEXT = "Tapped 2 times"
private const val CARD_TEXT_SIZE_SP = 16
private const val FOOTER_TEXT = "Served by the producer"
private const val FOOTER_TEXT_SIZE_SP = 14
private const val FOOTER_MAX_LINES = 2
private const val FOOTER_Y_DP = 128f
private val CARD_COLOR = Color(color = 0xFF6200EE)
private val FOOTER_COLOR = Color(color = 0xDE000000)

class ExampleOneReaderTest {
    @Test
    fun `given R20 example 1 as foundation code, when read, then it gives example 1's nodes without the click`() {
        // Arrange
        var cardText = IntSize.Zero
        var footerText = IntSize.Zero
        val spec = SceneSpec(widthPx = VIEWPORT_WIDTH_PX, heightPx = VIEWPORT_HEIGHT_PX, density = Density(DENSITY))

        // Act
        val actual = documentOf(spec = spec) {
            Column(Modifier.padding(SCREEN_PADDING_DP.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(CARD_HEIGHT_DP.dp)
                        .background(color = CARD_COLOR, shape = RoundedCornerShape(CARD_CORNER_DP.dp))
                        .clickable {}
                        .padding(SCREEN_PADDING_DP.dp),
                ) {
                    ScaledBasicText(
                        text = CARD_TEXT,
                        style = robotoStyle(sizeSp = CARD_TEXT_SIZE_SP, weight = FontWeight.Medium)
                            .copy(color = Color.White),
                        onTextLayout = { cardText = it.size },
                    )
                }
                Spacer(Modifier.height(SCREEN_PADDING_DP.dp))
                ScaledBasicText(
                    text = FOOTER_TEXT,
                    modifier = Modifier.fillMaxWidth(),
                    style = robotoStyle(sizeSp = FOOTER_TEXT_SIZE_SP).copy(color = FOOTER_COLOR),
                    maxLines = FOOTER_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { footerText = it.size },
                )
            }
        }.root.withoutIds()

        // Assert
        assertEquals(expected = expectedExampleOne(cardText = cardText, footerText = footerText), actual = actual)
    }

    private fun expectedExampleOne(cardText: IntSize, footerText: IntSize): DocumentNode.Box {
        val radius = CARD_CORNER_DP.toFloat()
        val padding = SCREEN_PADDING_DP.toFloat()
        val card = DocumentNode.Box(
            id = 0,
            x = padding,
            y = padding,
            width = CARD_WIDTH_DP,
            height = CARD_HEIGHT_DP.toFloat(),
            decoration = Decoration(
                background = DocumentColor(argb = 0xFF6200EEu),
                shape = Decoration.Shape.Rounded(
                    topStart = radius,
                    topEnd = radius,
                    bottomEnd = radius,
                    bottomStart = radius,
                ),
            ),
            children = listOf(
                DocumentNode.Text(
                    id = 0,
                    x = padding,
                    y = padding,
                    width = cardText.width / DENSITY,
                    height = cardText.height / DENSITY,
                    text = CARD_TEXT,
                    style = TextStyleSpec(
                        size = CARD_TEXT_SIZE_SP.toFloat(),
                        weight = FontWeight.Medium.weight,
                        color = DocumentColor(argb = 0xFFFFFFFFu),
                    ),
                ),
            ),
        )
        val footer = DocumentNode.Text(
            id = 0,
            x = padding,
            y = FOOTER_Y_DP,
            width = CARD_WIDTH_DP,
            height = footerText.height / DENSITY,
            text = FOOTER_TEXT,
            style = TextStyleSpec(
                size = FOOTER_TEXT_SIZE_SP.toFloat(),
                weight = FontWeight.Normal.weight,
                color = DocumentColor(argb = 0xDE000000u),
            ),
            maxLines = FOOTER_MAX_LINES,
            overflow = DocumentNode.Text.Overflow.Ellipsis,
        )
        return DocumentNode.Box(
            id = 0,
            x = 0f,
            y = 0f,
            width = VIEWPORT_WIDTH_DP,
            height = VIEWPORT_HEIGHT_DP,
            children = listOf(card, footer),
        )
    }
}
