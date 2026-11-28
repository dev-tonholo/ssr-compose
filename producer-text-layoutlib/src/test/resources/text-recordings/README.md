# Text recordings

What real Android versions measured when they laid out the same 360 texts. They are the expected values of the replay test of the layoutlib text engine ([#248](https://github.com/dev-tonholo/ssr-compose/issues/248)).

## Why the replay needs them

The producer may use layoutlib for text only while it matches layouts recorded on devices ([ADR 0026](../../../../../docs/adr/0026-producer-measures-text-with-layoutlib.md)). The expected values have to come from Android itself, never from the code under test. Every row of every file is kept because pixel values change with the Android version and the screen density, and the line height, line limit and ellipsis rules only show on some texts.

## Where each file came from

All nine were recorded on 2026-11-27.

| File | Android | Where it was recorded |
|---|---|---|
| `api30-emulator-step2.json` | API 30 | Emulator, Google APIs image |
| `api31-emulator-step2.json` | API 31 | Emulator, Google APIs image |
| `api32-emulator-step2.json` | API 32 | Emulator, Google APIs image |
| `api33-motorola-physical.json` | API 33 | Physical Motorola Edge 20 Pro |
| `api35-step2-emulator.json` | API 35 | Emulator |
| `api36-step2.json` | API 36 | Emulator, Google arm64 image `BE2A.250530.026.D1` |
| `api36-tablet-physical.json` | API 36 | Physical Samsung tablet (SM-P615) running LineageOS, not a Google image |
| `api37-emulator.json` | API 37 | Emulator, `android-37.2` Google APIs arm64 image with 16 KB pages |
| `api37-pixel7pro.json` | API 37 | Physical Pixel 7 Pro, recorded by the project owner |

## How they were recorded

`TextGateDeviceTest` in `contract-tests` drew each case with foundation `BasicText` (Compose `ui-text` 1.12.1) and wrote down what the device measured. The 360 cases are every mix of:

- Roboto and Open Sans regular, loaded from files
- 5 Latin and Greek samples
- 12, 16 and 24 sp
- font scale 1, 1.04 and 1.3, set through `LocalDensity`
- no line height, or 1.5 times the size
- no line limit, or 2 lines with an ellipsis

The box is always 200 dp wide.

The version of the test that records these 360 cases and their line details was never committed, so the test in this repo cannot write these files yet. For the Pixel file it is not known which state of that test code ran; its cases and fields are the same as in the other files. No file records the device's locale.

## Format

A header, then one row per line. In the header, `deviceModel` and `sdkInt` are there to name the source of the file; the replay reads only `density`. Of each row, only the fields the replay reads are kept. The other recorded fields were removed, and no kept value was changed. Sizes are in px, except in the fields whose names end in `InSp`. `textWidthInPx` is the width of the text's box, and `lineVisibleEnds` holds, for each line, the offset where its visible text ends.

The files are marked as generated in `.gitattributes`. Do not edit them by hand. A layoutlib or Compose upgrade needs new recordings, not new numbers in these.
