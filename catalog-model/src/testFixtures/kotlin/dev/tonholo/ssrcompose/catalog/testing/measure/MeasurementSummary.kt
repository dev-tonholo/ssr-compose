package dev.tonholo.ssrcompose.catalog.testing.measure

/** The median and the spread of repeated samples of one measurement. */
data class MeasurementSummary(val median: Double, val min: Double, val max: Double) {
    companion object {
        /** @throws IllegalArgumentException when [samples] is empty. */
        fun of(samples: List<Double>): MeasurementSummary {
            require(samples.isNotEmpty()) { "A measurement needs at least one sample" }
            val sorted = samples.sorted()
            val middle = sorted.size / 2
            val median = if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
            return MeasurementSummary(median = median, min = sorted.first(), max = sorted.last())
        }
    }
}
