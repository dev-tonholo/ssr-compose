package dev.tonholo.ssrcompose.core.document.internal

import dev.tonholo.ssrcompose.core.document.SlotSpace

private const val MAX_MEASURE = 100_000f
private const val MAX_SLOT_NAME_LENGTH = 64

/** The most slots an app component may have, the most parameters a bridge can have. */
private const val MAX_SLOTS_PER_COMPONENT = 118

/** A size the device reports: finite, not negative and at most 100,000 dp. */
internal fun requireMeasure(value: Float, what: String) {
    require(value in 0f..MAX_MEASURE) { "A $what must be from 0 to $MAX_MEASURE dp, was $value" }
}

/** At most [MAX_SLOTS_PER_COMPONENT] slots, each named with at most 64 characters. */
internal fun requireSlotSpaces(slots: Map<String, SlotSpace>) {
    require(slots.size <= MAX_SLOTS_PER_COMPONENT) {
        "A measurement holds at most $MAX_SLOTS_PER_COMPONENT slots, was ${slots.size}"
    }
    require(slots.keys.all { name -> name.length <= MAX_SLOT_NAME_LENGTH }) {
        "A slot name has at most $MAX_SLOT_NAME_LENGTH characters"
    }
}

/** A side of the space the app gives a screen: above 0 and at most 100,000 dp. */
internal fun requireViewportSide(value: Float, what: String) {
    require(value > 0f && value <= MAX_MEASURE) { "A $what must be above 0 and at most $MAX_MEASURE dp, was $value" }
}
