package dev.tonholo.ssrcompose.server.screen

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class MutableClock(var current: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = current
}
