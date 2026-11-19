# 0002. Use KSP in tests only

- Status: Accepted
- Date: 2026-11-18
- Modules: gradle-plugin

## Context

The compiler plugin creates a bridge when source code names a component. Generated code can name one too, so a test has to prove that the output of a real KSP processor is scanned. The project ships no KSP processor and has no other use for KSP.

## Options

1. Add KSP to the version catalog and use it only in a Gradle TestKit fixture. The test runs a real processor. The catalog gets one more version to keep up to date.
2. Use a Gradle task that writes a source file. This needs no dependency, but it does not prove that the folders of KSP are scanned.
3. Keep the KSP version in a test string. This needs no catalog entry, but it breaks the rule that versions live in the catalog.

## Decision

Option 1. KSP 2.3.14 and its `symbol-processing-api` are in `gradle/libs.versions.toml`, and only the fixture projects of `NamedBridgeToolchainTest` use them. KSP is Apache 2.0 licensed. No published artifact depends on it.

## Consequences

- Good: a real processor proves that generated sources create the bridges they call.
- Bad: a Kotlin upgrade can need a new KSP version before `functionalScaleTest` passes.
- Follow-up: none.
