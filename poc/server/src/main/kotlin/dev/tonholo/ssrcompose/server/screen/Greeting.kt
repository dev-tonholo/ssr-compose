package dev.tonholo.ssrcompose.server.screen

private const val NOON_HOUR = 12
private const val EVENING_START_HOUR = 18

fun greetingFor(hour: Int): String = when {
    hour < NOON_HOUR -> "Good morning"
    hour < EVENING_START_HOUR -> "Good afternoon"
    else -> "Good evening"
}
