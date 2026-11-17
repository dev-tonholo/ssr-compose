package dev.tonholo.ssrcompose.gradle.testing

/**
 * The sources of the Kotlin Multiplatform Android fixture: one common composable, two marked `expect` and `actual`
 * pairs, and one composable in each flavor. The files of the `Shared` pair have two names, and the files of the
 * `Panel` pair have the same name in the same package, which is a common layout.
 */
internal object KmpAndroidSources {
    const val HOME_PATH = "src/commonMain/kotlin/Home.kt"
    const val SHARED_EXPECT_PATH = "src/commonMain/kotlin/Shared.kt"
    const val SHARED_ACTUAL_PATH = "src/androidMain/kotlin/SharedActual.kt"
    const val PANEL_EXPECT_PATH = "src/commonMain/kotlin/Panel.kt"
    const val PANEL_ACTUAL_PATH = "src/androidMain/kotlin/Panel.kt"
    const val FREE_BANNER_PATH = "src/androidFree/kotlin/FreeBanner.kt"
    const val PAID_BANNER_PATH = "src/androidPaid/kotlin/PaidBanner.kt"

    val ALL: Map<String, String> = mapOf(
        HOME_PATH to markedComposable(declaration = "fun Home(greeting: String)"),
        SHARED_EXPECT_PATH to markedComposable(declaration = "expect fun Shared(title: String)", body = false),
        SHARED_ACTUAL_PATH to markedComposable(declaration = "actual fun Shared(title: String)"),
        PANEL_EXPECT_PATH to markedComposable(declaration = "expect fun Panel(heading: String)", body = false),
        PANEL_ACTUAL_PATH to markedComposable(declaration = "actual fun Panel(heading: String)"),
        FREE_BANNER_PATH to markedComposable(declaration = "fun FreeBanner(text: String)"),
        PAID_BANNER_PATH to markedComposable(declaration = "fun PaidBanner(text: String)"),
    )

    val EDITED_HOME: String =
        markedComposable(declaration = "fun Home(greeting: String, farewell: String = \"\")")

    val EDITED_FREE_BANNER: String =
        markedComposable(declaration = "fun FreeBanner(text: String, caption: String = \"\")")

    private fun markedComposable(declaration: String, body: Boolean = true): String = """
        package fixture.kmp

        import androidx.compose.runtime.Composable
        import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

        @ConsumerComposable
        @Composable
        $declaration${if (body) " {\n}" else ""}
    """.trimIndent() + "\n"
}
