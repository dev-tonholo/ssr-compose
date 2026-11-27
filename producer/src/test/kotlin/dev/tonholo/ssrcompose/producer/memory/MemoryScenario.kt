package dev.tonholo.ssrcompose.producer.memory

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.createFontFamilyResolver

/** One way of holding screens, each measured in a JVM of its own. [label] names it on the command line. */
internal enum class MemoryScenario(val label: String, val newSessions: () -> MemorySessions) {
    /** What the producer ships: a live session per screen. */
    Live(label = "live", newSessions = { liveSessions { seed -> MemoryScreen(seed = seed) } }),

    /** Live sessions that show nothing: what a session costs before its content. */
    LiveEmpty(label = "live-empty", newSessions = { liveSessions { } }),

    /** A scene per screen with no session around it and no read. */
    Hosts(label = "hosts", newSessions = { hostSessions { seed -> MemoryScreen(seed = seed) } }),

    /** As [Hosts], with every scene's texts resolving fonts through one resolver, which the producer does not do. */
    HostsSharedFonts(label = "hosts-shared-fonts", newSessions = ::sharedFontHostSessions),

    /** As [Hosts], with an empty box in place of each text. */
    HostsWithoutTexts(label = "hosts-no-text", newSessions = { hostSessions { MemoryScreenWithoutTexts() } }),

    /** A scene per screen, read once, with no session around it. */
    ReadHosts(label = "hosts-read", newSessions = ::readHostSessions),

    /** Every screen in one scene. */
    OneScene(label = "one-scene", newSessions = ::oneSceneSessions),

    /** The scene the producer used before, which keeps a raster surface per screen. */
    Drawn(label = "drawn", newSessions = ::drawnSessions),
    ;

    companion object {
        fun labelled(label: String): MemoryScenario =
            requireNotNull(entries.firstOrNull { it.label == label }) { "No memory scenario is named $label" }
    }
}

private fun sharedFontHostSessions(): MemorySessions {
    val sharedResolver = createFontFamilyResolver()
    return hostSessions { seed ->
        CompositionLocalProvider(LocalFontFamilyResolver provides sharedResolver) { MemoryScreen(seed = seed) }
    }
}
