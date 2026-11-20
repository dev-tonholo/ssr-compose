package dev.tonholo.ssrcompose.core.handshake.internal

import dev.tonholo.ssrcompose.core.handshake.FontScalePoint

private val POINT_COUNT = 1..32
private const val MAX_SP = 1_000f
private const val MAX_DP_PER_SP = 10f

internal fun requireCurvePoints(points: List<FontScalePoint>) {
    require(points.size in POINT_COUNT) { "A font scale curve has $POINT_COUNT points, was ${points.size}" }
    points.forEach { point -> requirePoint(point = point) }
    points.zipWithNext().forEach { (previous, next) ->
        require(next.sp > previous.sp) { "The sp sizes of a font scale curve must strictly increase" }
        require(next.dp >= previous.dp) { "The dp sizes of a font scale curve must never decrease" }
    }
}

private fun requirePoint(point: FontScalePoint) {
    require(point.sp > 0f && point.sp <= MAX_SP) { "An sp size must be above 0 and at most $MAX_SP, was ${point.sp}" }
    require(point.dp > 0f && point.dp <= point.sp * MAX_DP_PER_SP) {
        "A dp size must be above 0 and at most $MAX_DP_PER_SP times its sp, was ${point.dp}"
    }
}
