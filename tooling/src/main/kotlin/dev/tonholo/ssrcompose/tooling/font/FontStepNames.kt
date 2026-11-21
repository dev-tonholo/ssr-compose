package dev.tonholo.ssrcompose.tooling.font

/** The names of the font steps, the same in every build tool. */
public object FontStepNames {
    public const val BUNDLE: String = "ssrFontBundle"
    public const val CHECK: String = "ssrFontCheck"
    public const val DUMP: String = "ssrFontDump"

    /** The prefix of the per-variant task that writes the font version resource, such as `ssrFontVersionDebug`. */
    public const val VERSION: String = "ssrFontVersion"
    public const val SYNC: String = "ssrFontSync"
    public const val VERIFY: String = "ssrFontVerify"
    public const val RESOURCES: String = "ssrFontResources"
}
