package dev.tonholo.ssrcompose.core.handshake

import dev.tonholo.ssrcompose.core.handshake.internal.requireCurvePoints
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * How the device turns text sizes in sp into dp, sampled at [points]. Android 14 and later scale large text less
 * than small text, so the producer measures with this curve, not with a single font scale.
 *
 * @throws IllegalArgumentException when it has fewer than 1 or more than 32 points, an sp is not above 0 or is
 * above 1,000, the sp sizes do not strictly increase, a dp is not finite or not above 0, a dp is smaller than the
 * one before it, or a dp is over 10 times its sp.
 */
@Serializable
@JvmInline
public value class FontScaleCurve(public val points: List<FontScalePoint>) {
    init {
        requireCurvePoints(points = points)
    }

    public companion object {
        /** The text sizes in sp a device samples its curve at: the points of Android's own font scale tables. */
        public val SAMPLED_SIZES: List<Float> = listOf(8f, 10f, 12f, 14f, 18f, 20f, 24f, 30f, 100f)
    }
}
