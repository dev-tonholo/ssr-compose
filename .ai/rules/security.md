# Security

Rules for the connection, documents, sessions and secrets. The source is principles III, IV and V of the [constitution](../../.specify/memory/constitution.md).

## Rules

1. Traffic between consumer and producer is encrypted in transit. Cleartext is only allowed in local development builds.
2. End-to-end encryption is the goal. A design that does not reach it says why and what it protects instead.
3. The consumer validates a document before it renders it. A broken or hostile document never crashes the app.
4. The consumer rejects a document or message whose version it cannot read, with a clear error.
5. A session never reads or changes another session's state.
6. An action ID from a consumer is untrusted input. An unknown ID is ignored and never fails the session.
7. Handshake values from a consumer are untrusted input. Validate them before they size or shape anything.
8. Never log document bytes, user input or handshake details at a level that ships in a release build.
9. Never commit secrets, keystores, tokens or `local.properties`.
10. Keep personal names out of code, paths and fixtures.

## Known violations (do not copy them)

- `poc/app/src/debug/AndroidManifest.xml` allows cleartext traffic, and the PoC has no encryption at all.
- `poc/app/src/main/kotlin/dev/tonholo/ssrcompose/app/data/LoggingScreenService.kt` logs every tree and action. It is only wired in debuggable builds.

## Check

- `git diff --cached --name-only` shows no `.jks`, `.keystore`, `.pem`, `.env` or `local.properties` file.
- A change to the connection, handshake, documents or sessions has a reviewer report that names each rule in this file.

Agent: [reviewer](../agents/reviewer.md).
