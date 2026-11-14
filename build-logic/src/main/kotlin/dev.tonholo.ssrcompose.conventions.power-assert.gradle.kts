plugins {
    id("org.jetbrains.kotlin.plugin.power-assert")
}

powerAssert {
    functions.set(
        listOf(
            "kotlin.assert",
            "kotlin.test.assertEquals",
            "kotlin.test.assertTrue",
            "kotlin.test.assertFalse",
            "kotlin.test.assertNotNull",
        ),
    )
}
