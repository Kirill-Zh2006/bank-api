---
name: bank-service
description: Apply when adding a currency provider, changing transaction or limit behavior, creating a Flyway migration, or writing tests for this Spring Boot budgeting service.
---

# Bank service project guidance

Use this skill for changes to the bank budgeting service in this repository.

## Project map and conventions

- Java 21 and Spring Boot application code: `src/main/java/com/bank/demo`.
- REST controllers: `controller`; business services: `service` and `service/impl`.
- PostgreSQL schema changes belong in versioned Flyway migrations under `src/main/resources/db/migration`.
- API and persistence rules are described in `docs/data-model.md`; verify requirement status in `docs/requirements-checklist.md`.
- Tests use JUnit, PostgreSQL Testcontainers, and WireMock. Run them with `./gradlew test` (Docker must be available for Testcontainers).
- Use Java 21, `BigDecimal` for money, and the injected UTC `Clock` for current time. Keep REST response fields camelCase; transaction and limit request DTOs also accept snake_case aliases.

## Domain rules to preserve

- Store incoming transactions before resolving the exchange rate. If the provider fails, keep the transaction retryable instead of dropping it.
- USD amounts are assessed directly. For other currencies, use a cached USD rate whose date is on or before the transaction's UTC date; persist newly fetched rates.
- Monthly spending and limit selection use UTC. A missing custom limit means USD 1000.00.
- Select the limit effective at the transaction timestamp, not the current limit. Serialize assessment for a budget scope so concurrent operations share one balance.
- Obtain current timestamps through the injected `Clock` bean. The configured clock uses UTC.
- Existing JSON clients use camelCase. Keep it as the response format; input DTOs also accept documented snake_case aliases. Query parameters retain their existing snake_case names.

## Add or change a currency provider

1. Check the provider's official documentation and response examples before changing the response mapping. Keep HTTP details in `client/FxApiAdapter`; provider URL, key and timeout settings belong in `config/FxApiProperties` and `config/FxApiConfig`.
2. Map provider data in `dto/FxRateResponse`. Preserve `service/impl/CurrencyRateServiceImpl` behavior: use the latest cached rate on or before the transaction's UTC date, persist a newly fetched rate with its actual close date, and obtain `fetchedAt` from `Clock`.
3. Add WireMock coverage for the provider request, stored rate/date, cache reuse, and a failed request. The end-to-end fixture is `src/test/java/com/bank/demo/TransactionApiIntegrationTest.java`.

## Change limit or transaction behavior

1. Keep controllers thin. Transaction intake is in `TransactionController` and `TransactionIngestionServiceImpl`; limit operations are in `LimitController` and `LimitServiceImpl`; limit assessment is in `TransactionAssessmentServiceImpl`.
2. Preserve historical limit selection by transaction time, the USD 1000.00 fallback, UTC month boundaries, and the `budget_scope` lock around balance updates.
3. Extend `TransactionAssessmentServiceTest` for calculation boundaries. Add or update HTTP assertions in `TransactionApiIntegrationTest` or the relevant `*ControllerValidationTest` when the public contract changes.

## Create a database migration

1. Add a new file such as `src/main/resources/db/migration/V3__describe_change.sql`; never edit an applied migration.
2. Keep constraints and indexes in the schema. Entities and repository methods must match the migrated column names.
3. Verify the migration against the PostgreSQL Testcontainers setup by running `./gradlew test`.

## General change guidance

- Add schema changes as new Flyway migrations; do not edit an already-applied migration.
- Preserve the transaction status and retry flow when changing rate-provider behavior.
- Cover business behavior at the service level and externally visible behavior through the HTTP API where appropriate.
- For tests involving cached data, isolate assertions by account, currency, and rate date so results do not depend on test execution order.
- Update the checklist and relevant documentation when a requirement changes or becomes verifiably complete.
- MCP tools are in `com.bank.demo.mcp.BankMcpTools`; keep them read-only unless a future request explicitly calls for a state-changing operation.
