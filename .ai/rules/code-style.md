# Code style

Kotlin style for every module in this repository. The constitution's principle VIII and its "Technology and Platform Constraints" section are the source: [constitution](../../.specify/memory/constitution.md).

## Rules

1. Never use `!!`. Handle the null with `?.`, `?:`, `requireNotNull` or `checkNotNull`.
2. No wildcard imports. Import each name.
3. No fully qualified names in code. Add an import, or an import with an alias when two names clash, as `poc/app/src/main/kotlin/dev/tonholo/ssrcompose/app/MainActivity.kt` does for the two `GreetingCard` names.
4. Use named arguments wherever the call allows it.
5. No magic numbers or strings. Use named constants, as in `poc/server/src/main/kotlin/dev/tonholo/ssrcompose/server/screen/CounterBanner.kt`.
6. Use `$$"..."` strings when you need a literal `${...}`.
7. Keep functions small (about 20 lines), with at most 3 or 4 parameters and 2 or 3 levels of nesting. Return early.
8. One top-level class per file, unless the classes truly belong together, such as a sealed interface and its cases.
9. Follow SOLID, KISS, DRY and YAGNI. Prefer composition over inheritance. Build nothing a spec does not ask for.
10. Use coroutines for concurrency. Do not use `runBlocking` in production code.
11. Never assume input is small. Name a realistic upper bound for sessions, document size and update rate, and make the code hold at it.
12. Respect detekt. Shorten a name before you reach for `@Suppress`.

## Known violations (do not copy them)

- `poc/server/src/main/kotlin/dev/tonholo/ssrcompose/server/action/ActionRegistry.kt` and `poc/server/src/main/kotlin/dev/tonholo/ssrcompose/server/tree/LiveTree.kt` use `java.util.concurrent` atomics and maps.
- `poc/server/src/main/kotlin/dev/tonholo/ssrcompose/server/tree/LiveTree.kt` registers one global snapshot observer per session, which does not hold with many sessions.

## Check

- `grep -rn '!!' --include='*.kt' .` finds nothing in new code.
- `grep -rnE '^import .*\.\*$' --include='*.kt' .` finds nothing.
- `./poc/gradlew -p poc detekt` passes when you touched `poc/`.

Templates: [unit-test](../blueprints/unit-test.md).
