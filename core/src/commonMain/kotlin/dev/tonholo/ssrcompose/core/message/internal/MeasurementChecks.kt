package dev.tonholo.ssrcompose.core.message.internal

import dev.tonholo.ssrcompose.core.document.Measurement

private const val MAX_MEASUREMENTS = 10_000

internal fun requireMeasurements(components: List<Measurement>) {
    require(components.size <= MAX_MEASUREMENTS) {
        "A ComponentMeasured holds at most $MAX_MEASUREMENTS measurements, was ${components.size}"
    }
    val ids = HashSet<Int>()
    components.forEach { measurement ->
        require(ids.add(measurement.id)) { "The component ${measurement.id} is measured twice" }
    }
}
