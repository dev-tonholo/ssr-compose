# 0012. Style the default problem message with Material's shape of style parameters

- Status: Accepted
- Date: 2026-11-19
- Modules: `consumer`

## Context

The default problem message fixes its colours, font size, padding and spacing as private constants (#196). An app must be able to match its own look without rewriting the message. The owner said: "For the styling, we should follow the same approach the material components follows: style parameters in the composable, Defaults object with our defaults. We don't need to follow that exact rule, and we can have a better one that fits best with our library, but we must not hardcode these things."

`consumer` has no Material dependency. Its defaults must stay readable in light and dark theme. The design is in [research.md R19](../../specs/001-consumer-catalog-bridge/research.md#r19-text-the-user-reads-and-the-look-of-the-problem-message-196).

## Options

1. Material's shape. `ScreenProblemMessage` takes several style parameters itself: `colors`, `textStyle`, `contentPadding`, `spacing`, `maxContentWidth`, `retryContentPadding` and `retryBorderWidth`, each defaulting to a value of `ScreenProblemMessageDefaults`. The two colours are grouped in `ScreenProblemMessageColors`, built by the `@Composable` `ScreenProblemMessageDefaults.colors()`, as `ButtonColors` and `ButtonDefaults.buttonColors()` are. Familiar to Compose developers. Costs ten parameters on the composable.
2. One style object, `ScreenProblemMessageStyle`, filled by `ScreenProblemMessageDefaults.style(...)`. Four parameters on the composable and one place to add a field, but not the Material shape the owner asked for.
3. A style `CompositionLocal`, or style parameters on `SsrScreen`. The first hides the look from the call site. The second mixes the message into the screen's API.

## Decision

Option 1. The owner chose it on 2026-11-19 over option 2, which the first version of this design recommended. Colours are grouped because they change together with the theme and must stay a readable pair, so their default is a `@Composable` function. Every other value is a plain value in the `Defaults` object, as `ButtonDefaults.ContentPadding` is. No Material dependency is added.

## Consequences

- Good: an app changes one part of the look with one named argument, such as `colors = ScreenProblemMessageDefaults.colors(containerColor = ...)`, and keeps the rest.
- Good: the composable draws only the message and its retry button in the space it is given, so the library still owns no screen arrangement.
- Bad: ten parameters on one composable, and a new style value means a new parameter.
- Bad: `ScreenProblemMessageColors` has a public constructor, as `ButtonColors` has, so adding a colour later changes it.
- Bad: the defaults read the system theme. An app with its own theme switch passes its own `colors`.
- Follow-up: slice 3 in R19.
