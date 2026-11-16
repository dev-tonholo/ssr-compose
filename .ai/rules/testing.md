# Testing

How tests are written and where they live. The source is principle VI of the [constitution](../../.specify/memory/constitution.md).

## Rules

1. Write the test first, run it, and see it fail. Only then write the code.
2. Name JVM tests `given X, when Y, then Z` in backticks, as in `poc/server/src/test/kotlin/dev/tonholo/ssrcompose/server/screen/GreetingTest.kt`.
3. Name instrumented Android tests `given_x__when_y__then_z`. Spaces in method names are rejected below API 30. See `poc/app/src/androidTest/kotlin/dev/tonholo/ssrcompose/app/presentation/ui/components/FragmentPlayerTest.kt`.
4. Give every test body three parts, marked `// Arrange`, `// Act` and `// Assert`. Those are the only comments allowed in a test.
5. Use hand-written fakes named `Fake<Contract>`, as `poc/app/src/test/kotlin/dev/tonholo/ssrcompose/app/presentation/FakeScreenService.kt` does. Do not use a mocking library.
6. Use `kotlin.test` with named `expected =` and `actual =` arguments, Turbine for flows, and `runTest` with an injected dispatcher.
7. Anything both sides agree on has a contract test that runs the producer's output through the consumer's reader.
8. Test the compiler plugin with unit tests that compile sample sources.
9. Check rendering on a device or emulator while you work. A passing build is not proof that a screen is right.

| What | Folder | Command |
|---|---|---|
| All root modules | each module's test folders | `./gradlew build` |
| Core, on the JVM | `core/src/commonTest`, `core/src/jvmTest` | `./gradlew :core:jvmTest` |
| Core, as Android host tests | `core/src/commonTest`, `core/src/androidHostTest` | `./gradlew :core:testAndroidHostTest` |
| Core-testing, on the JVM | `core-testing/src/commonTest`, `core-testing/src/jvmTest` | `./gradlew :core-testing:jvmTest` |
| Core-testing, as Android host tests | `core-testing/src/commonTest`, `core-testing/src/androidHostTest` | `./gradlew :core-testing:testAndroidHostTest` |
| Contract tests | `contract-tests/src/androidHostTest` | `./gradlew :contract-tests:testAndroidHostTest` |
| PoC shared contract | `poc/protocol/src/test` | `./poc/gradlew -p poc :protocol:test` |
| PoC server | `poc/server/src/test` | `./poc/gradlew -p poc :server:test` |
| PoC app view model and data | `poc/app/src/test` | `./poc/gradlew -p poc :app:testDebugUnitTest` |
| PoC app UI | `poc/app/src/androidTest` | `ANDROID_SERIAL=<serial> ./poc/gradlew -p poc :app:connectedDebugAndroidTest` (needs a device) |

## Known violations (do not copy them)

- The class behind `createScreenService` in `poc/app/src/main/kotlin/dev/tonholo/ssrcompose/app/data/ScreenServiceFactory.kt` has no test. It needs a real socket.

## Check

- The report for a change shows the failing run before the passing run.
- No test file imports a mocking library.

Templates: [unit-test](../blueprints/unit-test.md).
