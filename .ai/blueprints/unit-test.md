# Blueprint: unit test

Rules: [testing](../rules/testing.md), [code-style](../rules/code-style.md).

## Steps

1. Put the test next to the code's package, under the module's `src/test/kotlin`.
2. Name the class `<Subject>Test` and each test `given X, when Y, then Z`.
3. Write the three parts: `// Arrange`, `// Act`, `// Assert`.
4. Run it and see it fail for the right reason.
5. Write the code, then run it again and see it pass.

## Template

```kotlin
class <Subject>Test {
    @Test
    fun `given <precondition>, when <action>, then <outcome>`() {
        // Arrange
        val <input> = <value>

        // Act
        val actual = <subject>(<name> = <input>)

        // Assert
        assertEquals(expected = <expected>, actual = actual)
    }
}
```

## Filled example from the code

`poc/server/src/test/kotlin/dev/tonholo/ssrcompose/server/screen/GreetingTest.kt` tests `greetingFor` in `poc/server/src/main/kotlin/dev/tonholo/ssrcompose/server/screen/Greeting.kt`:

```kotlin
@Test
fun `given hour 0, when greetingFor is called, then it returns Good morning`() {
    // Arrange
    val hour = 0

    // Act
    val actual = greetingFor(hour = hour)

    // Assert
    assertEquals(expected = "Good morning", actual = actual)
}
```

Run it with `./poc/gradlew -p poc :server:test`. It was run and passes.

For a flow, see `poc/server/src/test/kotlin/dev/tonholo/ssrcompose/server/tree/LiveTreeTest.kt`, which uses Turbine and `runTest`.
