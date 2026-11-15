# UI with Compose

Rules for Compose code and the required visual check. The consumer library renders with Compose, and the constitution asks for a composable friendly API.

## Rules

1. The consumer library is Kotlin Multiplatform with Android as its only target. Put everything that does not need Android in common code.
2. Public API does not expose an Android-only type where a common one exists.
3. A composable that draws takes `modifier: Modifier = Modifier` as its first optional parameter and applies it to its root, as `poc/app/src/main/kotlin/dev/tonholo/ssrcompose/app/presentation/ui/components/GreetingCard.kt` does.
4. Hoist state. A composable gets its data and its callbacks as parameters.
5. Keep sizes density independent. Never size anything in raw pixels.
6. The consumer app can supply its own error composable. Never hard-code the only error screen.
7. Look at every rendering change on a device or emulator before you report it done. Compare spacing, colour, text and behaviour, in light and dark theme.

## Visual check

Use the `android` CLI. Pass the device every time.

- Screenshot: `android screen capture --device=<serial> -o <file>.png`
- Tap: `android input --device=<serial> tap 'text=Add one'`
- Read the screen: `android layout --device=<serial>`
- Dark theme: `adb -s <serial> shell cmd uimode night yes`

## Required skills

| When you are | Use skill |
|---|---|
| Driving a device or emulator | `android-cli` |
| Writing layout and modifier parameters | `compose-modifier-and-layout-style` |
| Deciding where state lives | `compose-state-hoisting` |
| Writing Compose UI tests | `compose-ui-testing-patterns` |

## Known violations (do not copy them)

- `poc/server/src/main/kotlin/dev/tonholo/ssrcompose/server/screen/CounterBanner.kt` sizes its fragment in raw pixels, so it looks different on each screen density.
- `poc/app/src/main/kotlin/dev/tonholo/ssrcompose/app/presentation/ScreenRoute.kt` hard-codes the error screen.

## Check

- The report for a rendering change names the device and has a screenshot.
- `grep -rn 'android\.' --include='*.kt' <commonMain folder>` finds nothing in common code.
