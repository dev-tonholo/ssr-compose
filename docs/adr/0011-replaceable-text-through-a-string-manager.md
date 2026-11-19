# 0011. Replace the consumer's text through a string manager

- Status: Accepted
- Date: 2026-11-19
- Modules: `consumer`

## Context

The consumer library hardcodes the four texts it writes for the user (#196). An app may localize into languages the library does not ship. The owner said: "For the text, we must have something that can be changed by the consumer app. The reasoning is because the consumer app may provide localization for other languages that we don't provide."

Two of the four texts are written by the session, which runs outside composition. The library is Kotlin Multiplatform with Android as its only target, and its common code must not need Android. The design and its probes are in [research.md R19](../../specs/001-consumer-catalog-bridge/research.md#r19-text-the-user-reads-and-the-look-of-the-problem-message-196).

## Options

1. A key enum and a replaceable `@Composable` manager in a `CompositionLocal`, with the library's English manager as the default. `ScreenProblem` says what happened and the text is chosen in composition. Works in common code and with any target, and an Android app can back the manager with its own `R.string`. Costs a public enum, an interface, a `CompositionLocal` and two new `ScreenProblem` cases.
2. Android string resources that an app overrides by resource merging. Familiar to Android developers and no new types. Android only, needs `expect`/`actual` for every text and a `Context` in the session, matches overrides by name with no compile check, and the module's Android target packages no resources today (probe in R19).
3. Compose Multiplatform resources. Common code and per-language files, but a new dependency, and the library's generated `Res` gives an app no documented way to replace a text.

## Decision

Option 1, approved by the owner on 2026-11-19. It keeps everything in common code, needs no setup for an app that does not localize, and keeps today's English text as the default. The session no longer writes user text. Plurals keep the signature in the contract and are added with the first text that needs one.

## Consequences

- Good: an app can replace every library text in any language.
- Good: `ScreenProblem` tells an app whether the connection ended, the answer was unreadable, or the producer gave a reason.
- Bad: two new public types, a `CompositionLocal`, and two new `ScreenProblem` cases that an app's own `unavailable` must handle.
- Bad: a new key breaks the build of an app whose manager lists every key with no fallback. This is on purpose, so the app sees the new text to translate.
- Bad: the producer's `reason` texts are shown as given, and the manager does not translate them. The owner settled on 2026-11-19 that the producer writes them in the language the app sent, with English as the fallback (messages.md rule 7).
- Follow-up: slices 1, 2 and 4 in R19, and T085 in `tasks.md`, which names `AnswerUnreadable` for an unreadable message. The look of the message is [ADR 0012](0012-problem-message-style-parameters.md).
