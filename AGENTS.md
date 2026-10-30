# Repository instructions

This repository is a Java 21 / Spring Boot bank-budgeting service. Before changing its transaction, limit, currency-rate, persistence, or test code, read `.agents/skills/bank-service/SKILL.md` and the relevant sections of `docs/data-model.md`.

## Project layout

- REST controllers: `src/main/java/com/bank/demo/controller`
- Services and business rules: `src/main/java/com/bank/demo/service`
- PostgreSQL entities and repositories: `entity` and `repository`
- Flyway migrations: `src/main/resources/db/migration`
- Tests: `src/test/java/com/bank/demo`
- OpenAPI documentation is exposed at `/v3/api-docs`; the MCP server is exposed at `/mcp`.

## Domain constraints

- Keep transaction intake durable when a currency provider fails; failed rate lookups use `RETRYABLE_ERROR` and scheduled retries.
- Use cached rates dated on or before the transaction's UTC date. Use `Clock` for current time and preserve UTC month boundaries.
- A missing custom monthly limit is USD 1000.00. Choose the limit effective at the transaction timestamp and retain the budget-scope lock during assessment.
- Preserve camelCase response JSON and the documented snake_case aliases for incoming transaction and limit requests.
- MCP tools in `BankMcpTools` are read-only. Do not expose the unauthenticated MCP endpoint outside a trusted local environment without adding a security boundary.

## Verification

- Run the full suite with `./gradlew test`; Docker is required for PostgreSQL Testcontainers.
- Use WireMock for external exchange-rate behavior and PostgreSQL Testcontainers for migration and repository behavior.
- Add migrations instead of modifying migrations already applied to a database. Update `docs/requirements-checklist.md` when a requirement is verified.

## MCP in Codex

The project-scoped server is configured in `.codex/config.toml`. Codex loads project MCP configuration only for trusted workspaces. Start the application on port 8080 before using the `bank_budget_service` tools.
