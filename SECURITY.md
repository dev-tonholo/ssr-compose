# Security Policy

## Supported versions

Nothing is released yet. Once there are releases, this page will list which versions get security fixes.

## Reporting a vulnerability

Please do not report security problems in public issues, discussions or pull requests.

Report them privately through GitHub:

1. Open the [Security tab](https://github.com/dev-tonholo/ssr-compose/security) of this repository.
2. Choose "Report a vulnerability".
3. Describe the problem, how to reproduce it, and what an attacker could do with it.

You should get an answer within a week. If the report is confirmed, a fix is prepared in private and you are credited when it is published, unless you prefer not to be.

## Scope

This project sends UI and user actions between an app and a server. Reports about these areas are especially welcome:

- The connection between consumer and producer, and its encryption.
- Documents or messages that can crash the consumer or make it run something it should not.
- One session reading or changing another session's state.
