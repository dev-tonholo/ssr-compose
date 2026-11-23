# 0023. The producer converts text sizes with the font scale curve

- Status: Proposed
- Date: 2026-11-21
- Modules: `producer`, `compiler-plugin`, `gradle-plugin`, `consumer`

## Context

Android 14 and later scale text non-linearly. On the emulator at font scale 1.3 (API 36), 12 sp is drawn as 15.6 dp, 16 sp as 20.2 dp and 24 sp as 26.4 dp, where a straight scale gives 15.6, 20.8 and 31.2 dp. The device sends its curve in the handshake (`DeviceContext.fontScaleCurve`, `FontScaleCurve.spToDp` in `core`), and the owner chose that the producer measures text with it ([research.md R20](../../specs/001-consumer-catalog-bridge/research.md#r20-server-rendered-content-194), "Messages"). Principle I holds: the producer lays out and the device draws each node in the box it was sent, so a text the producer cannot measure as the device draws it is refused with `onScreenNotSendable`, never sent with a wrong size.

The first plan, a scene `Density` whose `TextUnit.toDp()` follows the curve, does not work on Compose Multiplatform 1.12.2 desktop. A layout node does not measure with the scene's `Density` object. `NodeCoordinator` forwards only two numbers (`density` reads `layoutNode.density.density` and `fontScale` reads `layoutNode.density.fontScale`, `NodeCoordinator.kt` lines 88 to 92), `ParagraphLayoutCache` keeps only those two in an `InlineDensity` (`InlineDensity.kt` line 33), and on desktop `FontScaling` is `FontScalingLinear` (`FontScacling.nonAndroid.kt` line 22). The Skia paragraph then turns the size into pixels with that rebuilt, linear density (`ParagraphBuilder.skiko.kt` line 331).

The owner then decided, on 2026-11-21, that the producer converts text sizes itself: every sp text size goes through the curve before layout, and the scene runs so the result is not scaled again. This record says how, with the owner's answers of 2026-11-21 to three follow-up questions: the compiler plugin rewrites server text, the curve is given through a composition local, and the document keeps sizes in sp that the device converts with its own curve.

## Probes

Run on 2026-11-21 on macOS arm64, in a throwaway copy of this branch outside the repository, with Kotlin 2.4.21, Compose Multiplatform 1.12.2 and the bundled Roboto Regular, at density 2.5 and font scale 1.3, with a curve through 12 sp to 15.6 dp, 16 to 20.2 and 24 to 26.4, as the emulator drew them. These match Compose's own table for font scale 1.3 (`FontScaleConverterFactory.android.kt` line 70, where 16 sp falls between 14 to 18.8 and 18 to 21.6). The probe's points above 24 sp (30 to 32, 100 to 104) are not that table's (30 to 30, 100 to 100), and no probed size used them. The expected heights come from Skia's own metrics of the Roboto file (descent minus ascent at the font size in pixels), not from a second layout. Only 16 and 24 sp tell the curve from a straight scale; at 12 sp both give 15.6 dp.

| Case | 12 sp | 16 sp | 24 sp |
|---|---|---|---|
| Expected on the curve | 45.7 px | 59.2 px | 77.3 px |
| Expected with a straight scale | 45.7 px | 60.9 px | 91.4 px |
| A. `BasicText`, scene `Density(2.5, 1.3)` | 46 | 61 | 91 |
| B. `BasicText`, scene with the curve `Density` (the first plan) | 46 | 61 | 91 |
| C. `LocalDensity(2.5, fontScale = curve(s) / s)` around one `BasicText`, scene `Density(2.5, 1.3)` | 46 | 59 | 77 |
| D. Style size changed to `curve(s).sp`, scene `Density(2.5, 1)` | 46 | 59 | 77 |
| E1. A Material-style `Text` that reads a `LocalTextStyle` the producer provides with `curve(s).sp`, no size of its own, scene font scale 1 | 46 | 59 | 77 |
| E2. The same `Text` with its own `fontSize = s.sp` | 35 | 47 | 70 |
| R. Server code calling the `String` overload of `BasicText`, compiled with a probe IR step that swaps the call for a copy of `BasicText` doing C, run before the Compose compiler; the copy read the curve from a stand-in composition local with the curve as its default | | 59 | 77 |
| R0. The same server code compiled with the Compose compiler only | | 61 | 91 |

More cases, same setup:

- J. One `LocalDensity` around a whole screen holding a 16 sp and a 24 sp text: with the factor of 24 sp (1.1) they were 52 and 77 px, with the factor of 16 sp (1.2625) 59 and 89 px, where the curve gives 59 and 77. A layout node takes one density, from the `LocalDensity` where it is created (`LayoutNode.kt` line 771, `density = value[LocalDensity]`), and a `Density` has one `fontScale`, so one factor per screen fits one size only. A composition local reaches layout only as that one density, so the factor has to be provided around each text.
- R2. The same probe IR step registered after the Compose compiler crashed the compile: the call to `BasicText` already had 13 arguments (the Compose compiler adds `$composer`, `$changed` and `$default`) against the copy's 10 parameters.
- F. An `AnnotatedString` at 16 sp with a 24 sp span, whose line should be 26.4 dp (77 px): 89 px with the factor of 16 sp (C), 70 px with only the base style converted (D), 91 px with neither. A span's own size is caught by neither. The reader already refuses text with several styles.
- G. Two lines at 16 sp with a line height of 24 sp: 135 px with the factor of 16 sp (C). Laid out at font scale 1 with 20.2 sp and Android's proportional line height, 24 / 16 x 20.2 = 30.3 sp, it is also 135 px; with the absolute rule, `curve(24)` = 26.4 sp, it is 126 px; with a straight scale, 138 px. So the per-text factor gives the line height Android gives when its rule is proportional.
- H. A semantics mark set by a wrapper on the modifier it passes to `BasicText` lands on the same unmerged semantics node as the text. A raw `BasicText` has no mark. `TextLayoutResult.layoutInput` reports the style as written (`fontSize` 16 sp) and the node's own density (`fontScale` 1.2625 inside the wrapper, 1.3 outside).
- I. `BasicText` with `autoSize` from 8 to 24 sp inside the factor of 24 sp chose 8 sp and kept font scale 1.1, so it was laid out at 8.8 dp where the curve gives 10.4 dp.
- An inline content placeholder (`appendInlineContent`, sized in sp) has no span style, so the reader's "several styles" check misses it today.

What no public API offers on Compose UI 1.12.2 desktop: foundation has no `LocalTextStyle`, and `BasicText` reads no default style (also found for [ADR 0020](0020-bundle-roboto-as-the-default-font.md)); `FontFamily.Resolver` resolves a typeface and never sees a size; `Paragraph` creation and `ParagraphLayoutCache` are internal.

How the device converts, read in `androidx.compose.ui:ui-unit-android` and `ui-text-android` 1.12.1:

- `FontScaling.toDp()` uses `FontScaleConverterFactory.forScale(fontScale)` when `fontScale >= 1.03` (`isNonLinearFontScalingActive`), and a straight scale below.
- The factory's five tables, for font scales 1.15, 1.3, 1.5, 1.8 and 2, are all keyed at 8, 10, 12, 14, 18, 20, 24, 30 and 100 sp (lines 57 to 94), the same sizes as `CommonFontSizes`, which it uses for the tables it interpolates between two font scales and between 1 and 1.15. Above its largest table it uses a straight scale (line 145).
- `FontScaleConverterTable.lookupAndInterpolate` interpolates linearly between two keys, from 0 below the first key, and with the last key's ratio above the last, the same rules as `FontScaleCurve.spToDp`.
- `resolveLineHeightInPx` (`SpannableExtensions.android.kt` lines 225 to 248) makes a line height in sp proportional to the converted font size when `fontScale > 1.05`, and converts it on its own (`lineHeight.toPx()`, through the curve) at 1.05 and below.

## Options

1. A scene `Density` that follows the curve. No server change. Does not work: case B lays out exactly like a straight scale.
2. A producer hook with no server change: Material's `LocalTextStyle` or `ProvideTextStyle`, a font resolver, or the paragraph pipeline. Only the first exists, and only for Material's `Text`, which the producer does not support. It catches a text with no size of its own (E1) and misses a text that sets one (E2) and every `BasicText`.
3. Server text goes through a library composable that server code calls in place of `BasicText`, and the reader refuses every text that did not. Exact, but server code changes `BasicText` to the library's composable, against R20's "plain foundation code".
4. The compiler plugin rewrites each call to `BasicText` in server code into the library's copy of it, which provides the factor of its size around the text, and the reader refuses every text that was not rewritten. Server code stays plain foundation code. Cost: an IR rewrite in the plugin that must run before the Compose compiler (R2), a copy of each `BasicText` overload in `producer` that follows Compose upgrades, and text in code compiled without the plugin (Material, a library module) is refused.
5. Linear on both sides: the scene keeps `Density(density, fontScale)`, and the consumer draws document text at font scale 1 with the size multiplied by the font scale. No server change, catches every text. Cost: at large font scales server text is drawn bigger than the app's own text (24 sp at 1.3 is 31.2 dp against the system's 26.4 dp), against the owner's choice to follow the device's curve.

For the document, two ways: keep sizes in sp and let the device convert them with its own curve, or send the laid-out sizes in dp and have the consumer draw them at font scale 1, so the device never converts. The second is exact with no condition on the device, but changes what the document's sizes mean.

## Decision

Option 4, chosen by the owner on 2026-11-21, with the factor given through `LocalDensity` around each text and sizes kept in sp in the document, also the owner's answers of that day.

### The library's copy of `BasicText`

`producer` gets `ScaledBasicText` in `dev.tonholo.ssrcompose.producer`, public and marked `@InternalSsrComposeApi` like `ConsumerComponent`, one overload per overload of `BasicText` that server code can call: the `String` one and the `AnnotatedString` one of foundation 1.12.2 (`BasicText.kt` lines 92 and 198; the six others are hidden). Each has exactly the parameters of its `BasicText` overload: the same names, types, order and default values. Each one:

1. takes `size`, the style's font size when it is in sp, or 14 sp when it is unspecified, and `curve`, from an internal composition local, `LocalScaledTextCurve: ProvidableCompositionLocal<FontScaleCurve?>`, null by default. The producer provides it once per screen, at the root of the scene's content, from the session's `Consumer.device.fontScaleCurve`, so a device change updates it. When it is null, outside a producer screen (a preview, the server developer's own Compose tests), the copy calls `BasicText` unchanged, with no provider and no mark;
2. calls `BasicText` with every argument unchanged inside `CompositionLocalProvider(LocalDensity provides Density(density = LocalDensity.current.density, fontScale = curve.spToDp(size) / size))`, so the text is laid out at `curve.spToDp(size)` dp and the scene's own density stays as it is;
3. leaves a line height in em as it is, since the document carries sp only and the reader refuses an em line height; when the line height is in sp and `device.fontScale` is from 1.03 to 1.05, passes the line height as `curve.spToDp(lineHeight) / factor` sp, so it is laid out at `curve.spToDp(lineHeight)` dp as the device lays it out in that range; elsewhere the factor alone gives the device's line height (G);
4. adds an internal semantics property, `ScaledTextMark(size, lineHeight)`, with the font size and line height as written, to the modifier it passes on;
5. calls `BasicText` with no provider and no mark when the size is in em, which the reader refuses as now.

This is the direct answer to the owner's question of 2026-11-21, "Can't we provide the font scale density using composition local?": the curve is provided once per screen through a composition local, and each text derives its own `LocalDensity` from it, since one density for the whole screen fits one size only (probe J). A device change recomposes each text with the new curve. The scene keeps `Density(density = device.density, fontScale = device.fontScale)` (`sceneDensity` as it is), so `LocalDensity.current.fontScale` in server code is the user's font scale.

### The rewrite

In modes `producer` and `check`, and never in `consumer` mode, the compiler plugin's IR step, registered by `SsrCompilerPluginRegistrar` itself and not inside `registerBridgeExtensions`, which returns early when a module has no catalog (`BridgeExtensionRegistration.kt` line 17), replaces each call whose callee is `androidx.compose.foundation.text.BasicText` with a call to the `ScaledBasicText` overload whose parameter types match, keeping every argument at its index, the call's type and its offsets. It runs in a module with no bridge too. It rewrites calls only; a function reference to `BasicText` is left as it is and is refused at run time.

- Order: the step must run before the Compose compiler, which the Gradle plugin sets with `-Xcompiler-plugin-order` (`gradle-plugin/.../PluginOrder.kt`). `PluginOrderGuard` today detects the wrong order only from functions the plugin created, and a module with screens and no bridge creates none. The rewrite adds its own check: a `BasicText` callee with a `$composer` parameter reports `PLUGIN_ORDER` and stops, instead of the crash of R2.
- A `BasicText` overload with no matching `ScaledBasicText`, after a Compose upgrade, is a compile error at the call that names the overload and says the library does not support it yet. It is never left as it is.
- The Kotlin toolchain plugin (T101) passes the same plugin order and mode, so the rewrite runs there too.
- Every server module that holds screens applies the Gradle plugin with `producer { }`, also with no catalog. Text in a module compiled without the plugin is refused at run time.

### The reader

For each text node, in this order:

1. Several styles, as now, and also any inline content placeholder (`layoutInput.placeholders` not empty): `SeveralStyles`.
2. A size in em: `RelativeFontSize`, as now.
3. A line height in em: the new reason `NotSendableReason.RelativeLineHeight`, at every font scale. Server text supports line heights in sp only; em support is tracked in #222.
4. No `ScaledTextMark`: the new reason `NotSendableReason.UnscaledText` ("a text compiled without the ssr-compose compiler plugin, such as Material's Text or a text from a library module, so it cannot follow the device's font scale"). This holds at every font scale, so a screen that works at font scale 1 never fails only for users with large fonts.
5. `layoutInput.density.fontScale` times the size differs from `curve.spToDp(size)` by more than 0.01 dp: `UnscaledText`. The curve is the one the scene was laid out with: `readScreen` gains a `curve` parameter, and `LiveScreen` passes the same `Consumer.device.fontScaleCurve` it provides to `LocalScaledTextCurve`. This catches a size changed after `ScaledBasicText` chose its factor, such as `autoSize` (I).

The document's `TextStyleSpec.size` and `lineHeight` are the mark's values, in sp as the server wrote them. The consumer draws them with its own `Density`, as R20 says.

### When the device draws what the producer measured

Sizes stay in sp and the device converts them with its own curve. That is exact when all of these hold:

1. The consumer samples the curve with the `Density` it draws with. Then at the sampled sizes the curve is the device's converter by construction.
2. Between and beyond the sampled sizes, the device's converter and `FontScaleCurve.spToDp` interpolate the same way. True on Compose UI Android 1.12.1, where the converter's keys are `SAMPLED_SIZES` and its rules are `spToDp`'s (see "How the device converts"). A Compose upgrade that changes `CommonFontSizes` or the interpolation breaks it, so a consumer test compares `spToDp` with the device's `Density.toDp` at sizes between and beyond the points.
3. The line height rule matches: proportional above font scale 1.05, curve of its own from 1.03 to 1.05, straight below 1.03, which `ScaledBasicText` follows.
4. The fonts are the same files ([ADR 0017](0017-app-fonts-travel-in-a-pinned-font-bundle.md), [ADR 0020](0020-bundle-roboto-as-the-default-font.md)), and Skia and Android's text engine measure them alike, which the text gate (T123) checks.

A font scale change during a session: Android treats it as a configuration change. When the activity is recreated, the session starts again with a new `Introduce`. When the app keeps its activity, the consumer's `Density` changes at once and the device draws the last document's sp sizes with the new curve, in the old boxes, until the consumer has sent `DeviceChanged` with the new curve and the producer's next revision arrives, one round trip later. In between, a text that grew is clipped in its box and one that shrank leaves space; no node moves.

## Consequences

- Good: every text in a document is measured as the device draws it, or the screen is refused. Text follows the device's own curve, like the app's own text, and its line height follows Android's rule.
- Good: server code stays plain foundation code with `BasicText`, and `LocalDensity.current.fontScale` in server code is still the user's font scale.
- Bad: the compiler plugin rewrites Compose calls, so it must run before the Compose compiler in every server module, also one with no bridge, and `ScaledBasicText` must follow each `BasicText` overload on every Compose upgrade.
- Bad: Material's `Text`, any text from a module compiled without the plugin, a function reference to `BasicText`, `autoSize` and inline content stop the screen with `onScreenNotSendable`.
- Bad: sp used outside a text, for example `with(LocalDensity.current) { 16.sp.toDp() }` for a padding, converts with the straight font scale (20.8 dp at 1.3, where the curve gives 20.2). The box is sent in dp and drawn as sent, so nothing moves, but it does not follow the curve. Server code that wants the curve reads `LocalConsumer.current.device.fontScaleCurve.spToDp`. A modifier passed to `BasicText` sits on the text's own node, so sp inside it converts with that text's factor.
- Bad: exactness depends on the device's converter matching the sampled curve (conditions 1 to 3), and a font scale change while the activity stays clips text for one round trip.
- Bad: what the reader cannot see is still lost without a report: text drawn with `drawText` and a `TextMeasurer`, or text under `clearAndSetSemantics`, like other custom drawing.
- Unchanged: letter spacing, a size in em and text with several styles stay refused.
- Unproven: Android drawing the same heights as the producer at 12, 16 and 24 sp and font scales 1 and 1.3, and the line height rule in the band from 1.03 to 1.05. The text gate (T123) proves them, with a run at font scale 1.04 added for line heights.
- Unproven: the rewrite of the `AnnotatedString` overload (probe R covered the `String` one), that a server module with `producer { }` and no catalog compiles in `producer` mode through the Gradle plugin (read in `SsrComposeExtension.resolveMode` and `BridgeExtensionRegistration.kt`, not run), and that an activity recreated after a font scale change starts the session again with a new `Introduce` (not run).
- Follow-up: T122 builds `ScaledBasicText`, the mark and the reader's checks; T130 builds the rewrite and its order check; T123 runs the gate with server text written as `BasicText` and rewritten.
