# Bank Budget Service

A Java 21 / Spring Boot service for accepting bank transactions, applying monthly spending limits, converting transaction amounts to USD, and reviewing transactions that exceeded their applicable limits.

## What the service does

- Stores each incoming transaction in PostgreSQL before requesting an exchange rate.
- Tracks spending separately for each source account and expense category (`product` or `service`).
- Applies immutable USD limit history. A transaction uses the latest limit effective at its transaction time; if no custom limit applies, the default is **USD 1,000.00**.
- Converts non-USD transaction amounts using a cached rate for the transaction date, or the latest cached rate dated before it. Converted amounts are rounded to two decimal places.
- Calculates monthly spending using UTC boundaries and marks an operation exceeded when cumulative spending is greater than the applicable limit. Spending equal to the limit is not an exceedance.
- Serializes assessments for the same account/category so concurrent transactions share the same monthly balance.
- Keeps an operation when the exchange-rate provider fails. It is marked `RETRYABLE_ERROR` and retried by the scheduled processor. The provider client uses a 3-second connect timeout and a 5-second read timeout.

PostgreSQL schema changes are managed with Flyway migrations. The core flow and API are covered by unit, integration, and HTTP tests; the integration suite uses PostgreSQL Testcontainers and WireMock.

## Requirements

- Java 21
- Docker, for the development PostgreSQL service and Testcontainers tests
- An fxapi API key for starting the application; set it in the `FXAPI_API_KEY` environment variable

## Run locally

Set the API key and start the application from the project directory:

```bash
export FXAPI_API_KEY="your_api_key"
./gradlew bootRun
```

The development Docker Compose integration starts PostgreSQL from `compose.yaml`. The application listens on port `8080` by default.

## REST API

Swagger UI: `http://localhost:8080/swagger-ui/index.html`  
OpenAPI document: `http://localhost:8080/v3/api-docs`

### Accept a transaction

`POST /api/transactions` returns `202 Accepted` with the transaction ID and processing status.

```json
{
  "accountFrom": "1234567890",
  "accountTo": "0987654321",
  "currencyShortname": "USD",
  "sum": 25.50,
  "expenseCategory": "product",
  "datetime": "2026-10-30T12:00:00Z"
}
```

Account numbers must contain exactly ten digits, amounts must be positive, and categories are `product` or `service`.

### Set and list limits

`POST /api/limits` creates a new limit. Its effective timestamp is assigned by the service; clients do not provide one. Limits are append-only.

```json
{
  "account_from": "1234567890",
  "expense_category": "product",
  "limit_sum": 500.00
}
```

`GET /api/limits?account_from=1234567890` returns the account's limit history. Limits are always denominated in USD.

### List exceeded transactions

`GET /api/transactions/exceeded?account_from=1234567890&expense_category=product` returns operations that exceeded the applicable limit, including the limit amount, currency, and effective date when a custom limit applied.

## Run tests

Run the complete test suite with Docker available:

```bash
./gradlew test
```

## AI in the project

### Tool and use

OpenAI Codex was used as the project AI agent to compare the implementation with the assignment, navigate the existing code, prepare changes and tests, and analyze build results. Requirements and domain decisions were checked against the assignment PDF, project documentation, and test results.

### MCP server

Codex connects to the project-scoped MCP server `bank_budget_service`, configured in `.codex/config.toml`. The Spring application exposes a Streamable HTTP endpoint at `/mcp` with two read-only tools:

- `get_limits` returns the limit history for a ten-digit account number.
- `get_exceeded_transactions` returns exceeded transactions for an account and the `product` or `service` category.

For example, to investigate an account's limit usage, an agent can retrieve its limit history and then its exceeded transactions, and explain the result using the returned data. The Codex configuration enables only these two tools and asks for confirmation before a call. To use them, open this folder as a trusted project in Codex, reload the MCP configuration, and start the application on port `8080`. The MCP endpoint has no authentication; keep it in a trusted local environment unless an authentication boundary is added.

### Agent Skill

The project Skill is `.agents/skills/bank-service/SKILL.md`. It documents the project structure, commands, and rules for transaction processing, limits, exchange rates, and migrations. For example, a request to add an exchange-rate provider directs the agent to `client/FxApiAdapter`, `config/FxApiProperties`, `config/FxApiConfig`, `dto/FxRateResponse`, and `service/impl/CurrencyRateServiceImpl`; it also asks the agent to check the provider's official response format and add WireMock tests. `AGENTS.md` tells Codex when to apply this Skill.

Open the project in Codex and describe the service task. In a trusted project, Codex can discover Skills under `.agents/skills/`; `AGENTS.md` points it to the relevant project Skill.

### Human work and checking AI output

The requirements and domain decisions were reviewed by the developer. In particular, the developer implemented the limit-history behavior in `LimitServiceImpl.getLimits` after discussing its contract; AI assisted with project navigation and test scenarios. AI-prepared changes were reviewed in code and checked by running tests.

During development, AI proposed some tests that were not isolated from data created by other scenarios and initially relied on an unverified assumption about JSON field names. These issues were found by checking the API contract and assignment, and by compiling and running HTTP tests; the checks were corrected and rerun. The requirements checklist also contained stale statuses, so its entries were updated only after checking the relevant code and tests.
