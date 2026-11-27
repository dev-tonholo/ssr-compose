# 0025. The device measures server text and reports its sizes

- Status: Not chosen; since 2026-11-27 fallback D of [ADR 0026](0026-producer-measures-text-with-layoutlib.md), which was accepted with option L2b, text measured with layoutlib's Android text classes. Only slice M0 was approved and built.
- Date: 2026-11-26
- Modules: `core`, `producer`, `consumer`, `contract-tests`

## Context

The producer lays out every screen in an `ImageComposeScene` and the device draws each node in the box it was sent ([ADR 0015](0015-producer-renders-with-compose-ui-foundation.md)). Probe P21 ([research.md R20](../../specs/001-consumer-catalog-bridge/research.md#text-gate-t123)) replayed 60 texts the emulator drew through the real producer and 10 missed the text gate's rule: each line is 1 px taller or shorter on Skia than on Android, which adds up with the line count, and one Greek string is 525.9 px wide on the producer in a 525 px box, so it wraps where the device does not. No producer setting can promise that two text engines break and round lines alike. On 2026-11-26 the owner chose the fallback R20 named for this case: the device reports text sizes back to the producer, as it does for app components ([ADR 0016](0016-app-components-measured-on-the-device.md)). The design is in R20, "Text measured on the device (#194)".

Probes P23 to P25 (R20, "Probes") ran the producer side with a fake device, Skia with the font size changed by 3%, 0.5% and -0.5%. After the producer applied the fake device's answers, every text in 13 layouts was at exactly the box and position the fake device lays it out at on its own, 39 runs of 39, including rows with weights, intrinsic sizes, baseline alignment, `maxLines` with an ellipsis, a bounded height, a minimum width and centred text. Every layout settled in 1 or 2 round trips. That the device's own measurement equals what it then draws is not proven on a device; slice M0 proves it first.

## Options

| | (a) Every text waits for the device | (b) Show the producer's layout at once, the device corrects | (c) Match Android's line rounding on the producer, relax the line count rule |
|---|---|---|---|
| What the user sees first | The loading state, or the last shown screen, until the device answered | The screen at once with the producer's boxes; a text the device measures differently is clipped or leaves space | The screen at once with the producer's boxes |
| Added delay before a screen with text is first shown | 1 round trip, 2 when a wrapping text's width depends on a sibling's measured width (P25: 4 of 13 layouts). A screen that already waits for app components gets its text answers in the same rounds, and needs one more only for a wrapping text whose width depends on a component's reported size (not probed) | 0 | 0 |
| Delay on an update | 0 when every text and width is remembered; else 1 or 2 round trips, while the last screen stays | 0, then the boxes move 1 or 2 round trips later | 0 |
| Correct boxes | Always, once shown (except the bounded fallbacks of R20) | After 1 or 2 round trips. In P21, 16 of 60 single texts were off by at least 1 px, so most screens with several texts move once | Never promised: line breaks at the edge of a box still differ (P21: 1 of 60), and the line rounding is a fit of 60 rows, not read in Skia's code |
| Layout shift | None | Once per screen or update that holds a text measured differently | None, but some text is clipped or leaves space |
| Wire change | `measure` on pending text nodes, `TextMeasured` from the device | The same, plus the producer's own answer for every query, and a "measured, no difference" message so silence means something | None |
| Testability | A screen is never shown with a text the device did not measure: one rule, checked by tests on both sides | Transient wrong frames are by design; tests must allow them | Only statistical: a sample of texts per device and Android version |
| Merged work kept | Most of it (R20 table); the gate is dropped | The same as (a) | All of it, plus a producer change to line heights and an owner change to the gate's rule |
| Risk | More round trips on deep layouts and on texts that change faster than a round trip, bounded by `measuringRounds` and `textPendingLimit` | A visible jump on most screens | Wrong text on some devices, found by users |

## Decision

Proposed: option (a). The table is weighed for control and testability, the general preference the owner gave for weighing it, not a choice of an option. With (a), every text box of a shown screen is one the device measured, or one Compose's own width rule derives from the device's measurements of that text (R20, rule 1, checked on the device by slice M0), which one rule enforces and tests can check on both sides. It costs one round trip before a screen with text is first shown, and two when a wrapping text's width depends on a sibling or an app component; a screen that already waits for app components gets its text answers in the same rounds. Remembered sizes make most updates free. On 2026-11-26, after reading this table, the owner answered "Yes both": yes to the recommendation that text waits for the device, option (a), and yes to running slice M0 on the tablet now. That approves building slice M0 only. Slices A0 and M0b to M6 are not approved yet, and neither are the `textPendingLimit` fallback, the refusal of `onTextLayout` and of `minLines` above 1, whole pixels on the wire for text, and the removal of PR #212's recorder and recorded file (R20, "Text measured on the device (#194)"). This record stays Proposed until the owner approves the rest.

## Consequences

- Good: every shown text box is what the device measured, or derived from its measurements by Compose's own rule, so the text gate's two failure causes cannot happen on a shown screen, outside the bounded fallbacks.
- Good: the producer still arranges every screen. The device reports only sizes of each text under constraints the producer chose (R20, "Principle I").
- Good: merged work stays useful. `ScaledBasicText` and the compiler plugin's rewrite ([ADR 0023](0023-producer-converts-text-sizes-with-the-font-scale-curve.md)) become the hook that applies the device's sizes to every server text; the font scale curve and the fonts keep the producer's first layout close, so few widths need a second round.
- Bad: a screen with text is first shown one round trip later, and two in some layouts. A screen with no app component, which showed at once before, now waits one round trip too. A text that changes faster than a round trip is shown with the producer's own size after `textPendingLimit`, and reported.
- Bad: new wire shape, a new message, a size cache per session and new limits, all untrusted input from the device.
- Bad: that the device's measurement equals what it draws is a device fact, unproven until slice M0.
- Follow-up: [ADR 0024](0024-text-gate-producer-half-in-contract-tests-jvm.md) and the text gate design are dropped. ADR 0023's "When the device draws what the producer measured" no longer carries correctness.
