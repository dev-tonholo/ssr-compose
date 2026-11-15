# Contributing to ssr-compose

Thanks for taking the time to contribute. This page explains how to ask a question, report a bug, suggest a change, and send code.

By taking part in this project you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Where the project is

The library is still being designed, and nothing is released. The proof of concept in [poc](poc) is a reference and is not held to the library's standards. Because of that, the most useful contributions right now are questions, ideas and reviews of the design.

The rules the project is built by are in the [constitution](.specify/memory/constitution.md). Please read it before proposing a change. A change that goes against it needs the constitution to be amended first.

## I have a question

- Look through the [open issues](https://github.com/dev-tonholo/ssr-compose/issues) first.
- If nothing answers it, open a new issue and say what you are trying to do.

## Reporting a bug

Before you report:

- Make sure you are on the latest commit of `main`.
- Check that it was not reported already.

A good report has:

- What you did, step by step.
- What you expected and what happened.
- Versions: Kotlin, Android Gradle Plugin, the device or emulator and its Android version.
- Logs, a stack trace or a screenshot when they help.

Use the bug report template when you open the issue.

Security problems are different. Do not open a public issue for them. Follow [SECURITY.md](SECURITY.md).

## Suggesting a change

Open an issue with the feature request template. Say what problem you have, not only the solution you have in mind. Features here start from a written spec, so a clear problem statement is the best starting point.

## Working on code

### Setup

You need JDK 17 or newer. The proof of concept also needs the Android SDK.

```
git clone git@github.com:dev-tonholo/ssr-compose.git
cd ssr-compose/poc
./gradlew build detekt
```

The repository root is a Gradle build with the library modules. Build and test them from the root with `./gradlew build`. The commands above are for the proof of concept in `poc/`, which has its own build.

### How work is done

- Features go through [Spec Kit](https://github.com/github/spec-kit): a spec, a plan, tasks, then the code.
- Tests come first. Write the test, see it fail, then write the code.
- Test names follow `given X, when Y, then Z`, and test bodies have Arrange, Act and Assert parts.
- Tests use hand-written fakes, not mocking libraries.
- Anything that draws on the screen is checked on a device or emulator, not only by a passing build.

### Code style

- Kotlin, checked with detekt.
- No `!!`, no wildcard imports, named arguments where possible.
- Code should explain itself. Comments are for short API docs, formulas, attributions, and logic that truly cannot be simplified.
- Keep the public API small. A public API change is its own pull request.

### Commits

Use [Conventional Commits](https://www.conventionalcommits.org/), in small steps:

```
feat(consumer): send the catalog version in the handshake
fix(producer): refuse an unsupported contract version
docs: explain the document format
```

Keep the message to one line when you can. Use `!` after the type or scope for a breaking change.

### Pull requests

- Branch from `main` and rebase on it. History stays linear, with no merge commits.
- Keep a pull request to one topic.
- Say what changed and why, and how you checked it.

Before you ask for a review, check that:

- The build and detekt pass.
- New behaviour has tests, written first.
- Screen changes were checked on a device or emulator.
- Docs are updated when behaviour changes.
- The change follows the constitution.

## License

By contributing, you agree that your contributions are licensed under the [Apache License, Version 2.0](LICENSE).
