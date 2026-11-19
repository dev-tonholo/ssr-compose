package dev.tonholo.ssrcompose.consumer.testing

import androidx.compose.ui.unit.Dp
import kotlin.test.assertEquals

fun assertDpEquals(expected: Dp, actual: Dp) {
    assertEquals(expected = expected.value, actual = actual.value, absoluteTolerance = ROUNDING_TOLERANCE_IN_DP)
}
