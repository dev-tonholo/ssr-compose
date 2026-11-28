package dev.tonholo.ssrcompose.producer.text.layoutlib

/** Measures text the way Android lays it out. Safe to call from several threads. */
internal interface TextEngine {
    /**
     * Loads a font from the content of a font file. Keep the result and share it between calls and threads.
     *
     * @throws IllegalArgumentException when [bytes] are empty or not a font the engine can read.
     */
    fun loadFont(bytes: ByteArray): TextFont

    /** Lays [request] out. Its font must come from [loadFont] of this kind of engine. */
    fun measure(request: TextRequest): TextMeasurement
}
