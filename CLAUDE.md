# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Anti-DDD Bank Demo — a bank management system redesigned from a traditional Spring DDD project using pure-function architecture. The original `spring-ddd-bank-master` used rich/anemic domain objects with JPA entities and injected repositories. This project separates all business rules into pure static functions, projects external state into fixed-size fact records, and keeps orchestration thin.

- **Language**: Java 17
- **Build**: Maven 3.6.3+, Spring Boot 3.0.13
- **Persistence**: JPA (Hibernate 6.1) + SQLite (`bank.db`)
- **Web**: Spring Boot embedded Tomcat, static HTML/CSS/JS on port 8080
- **JDK path**: `C:/Users/zhjun/Desktop/zulu17.66.19-ca-jdk17.0.19-win_x64`
- **Maven path**: `C:/Users/zhjun/Desktop/apache-maven-3.6.3`

## Commands

```bash
# Set up environment
export JAVA_HOME="C:/Users/zhjun/Desktop/zulu17.66.19-ca-jdk17.0.19-win_x64"
export PATH="$JAVA_HOME/bin:$PATH"
MVN="C:/Users/zhjun/Desktop/apache-maven-3.6.3/bin/mvn"

# Build
$MVN compile

# Run all tests (67 pure-function tests, zero mocks)
$MVN test

# Run a single test class
$MVN test -Dtest=TransferDecisionTest

# Run a single test method
$MVN test -Dtest=TransferDecisionTest#savingsCannotOverdraw

# Start the application (remove bank.db first for a clean state)
rm -f bank.db && $MVN spring-boot:run

# The app serves at http://localhost:8080/
# Static web UI at http://localhost:8080/ (auto-served from src/main/resources/static/)
# Demo credentials — Banker: banker / banker123; Client: registered by Banker
```

## Architecture

### Data flow (every business operation follows this pattern)

```
Transport (Controller) → Orchestration (Service) → Pure Function (Decision)
                                ↑                        │
                          loads facts              returns Result
                          from Stores              (allowed, reason,
                                │                 computed data)
                          ┌─────┴─────┐
                          │   Stores   │  ← side-effect: JPA → SQLite
                          └───────────┘
```

Multiple pure functions can be chained before effects execute. Example — a transfer:

```
Load facts → TransferFeeDecision.decide() → TransferDecision.decide() → persist both balances + log
```

### Layers (top to bottom)

| Layer | Package | Role | Framework dependency |
|-------|---------|------|---------------------|
| **Transport** | `bank.transport` | Parse HTTP, auth, delegate, format responses | Spring MVC `@RestController` |
| **Orchestration** | `bank.service` | Load facts → call pure function(s) → execute effects | Spring `@Service`, `@Transactional` |
| **Pure decisions** | `bank.domain.decisions` | Business rules as `static` pure functions | **None** |
| **Fact records** | `bank.domain.facts` | Plain Java records/enums for business data | **None** |
| **Stores** | `bank.repository` | Side-effect: project DB rows into facts | Spring `@Component`, JPA |
| **JPA entities** | `bank.repository.entity` | Infrastructure-only: map tables to rows. **Never cross into domain** | JPA `@Entity` |

### Pure business decisions (8 operations)

Each decision is a `final class` with:
- A `record Facts(...)` — fixed-size scalar/boolean/enum inputs
- A `record Result(...)` — complete computed output
- A `static Result decide(Facts)` method — the pure computation

| Decision | Facts shape | Business rules |
|----------|------------|----------------|
| `CreateClientDecision` | `(username, alreadyExists)` | Pattern `[a-z_A-Z][a-z_A-Z0-9]{0,30}`, uniqueness |
| `DeleteClientDecision` | `(ownsAnyAccount)` | Cannot delete if owns accounts |
| `DepositDecision` | `(amount, currentBalance, destExists)` | Amount > 0, computes new balance |
| `TransferDecision` | `(amount, totalFee, hasAccessRight, sourceBalance, destExists, destBalance, sourceAccountType)` | Amount > 0, access check, checking min −1000€ / savings no overdraft, computes both new balances |
| `TransferFeeDecision` | `(amount, isInternal, sourceAccountType, monthlyTransferCount, FeeRules)` | Internal flat / external flat+%, savings excess penalty (≥7/month), large-transfer tax |
| `AddAccountManagerDecision` | `(requesterIsOwner, managerAlreadyHasAccess)` | Only owner can add, no duplicates |
| `InterestCalculationDecision` | `(balance, days, yearToDateInterest, InterestRules)` | Gross = balance × rate / 365 × days, tax exemption, net = gross − tax |
| `BankRuleConfigDecision` | `(ruleKey, proposedValue)` | Validates against 8 rule-specific [min, max] ranges |

`FeeRules` and `InterestRules` are nested fixed-size records inside their respective decision classes — extracted only after genuine repetition appeared.

### Account types

`AccountType` enum — `CHECKING` (overdraft to −1000€, no interest) and `SAVINGS` (no overdraft, earns interest, 6 free transfers/month).

### Stores and JPA

Stores are the **only** classes that touch JPA entities. They:
1. Query via Spring Data JPA repository interfaces (`*JpaRepo`)
2. Map `Entity → Fact` mechanically via `toFact()`
3. Never contain business judgment
4. Return facts shaped to what the pure function needs (`boolean`, `int`, `Amount`, `Optional<Fact>`)

**SQLite caveats**:
- `existsBy*` derived queries are avoided — they generate `FETCH FIRST n ROWS ONLY` which SQLite doesn't support. Use `findBy*().isPresent()` or `countBy*() > 0` in Stores instead.
- Custom `SQLiteDialect` in `bank.repository.entity` provides identity-column support only.
- DDL is manual `schema.sql` (not Hibernate auto-DDL) because SQLite doesn't support `ALTER TABLE DROP CONSTRAINT`.

**Bank rules** are seeded by `DataSeeder` (runs on `ApplicationReadyEvent`, not `@PostConstruct` — to avoid race with schema.sql execution). Rules are read via `BankRuleStore.getAll()` → `Map<String,Double>`.

**Transfer and interest logs** are side-effect-only audit tables. They never enter the domain. `TransferLogStore` provides `countThisMonth()` for fee decisions and `findByAccounts()` for client transfer history.

### Authentication

| Role | Mechanism | Scope |
|------|-----------|-------|
| **Client** | `X-Username` header | All `/client/*` endpoints |
| **Banker** | `POST /bank/login` → token → `X-Banker-Key` header | Mutation endpoints (`POST/PUT/DELETE /bank/*`) |
| **Public** | None | `GET /bank/rules`, `GET /bank/transfers`, `GET /bank/client` |

Banker demo credentials: `banker` / `banker123`. Banker mutation endpoints check `X-Banker-Key` via `requireBanker()` in `BankController`.

### Web UI

Three screens managed by JS screen switching (`#screen-login` → `#screen-client` / `#screen-banker`). Static files in `src/main/resources/static/` follow the same pure/side-effect separation:

- **Pure functions** (namespace `F.*`): `F.bal()`, `F.typeBadge()`, `F.accountsTable()`, `F.rulesTable()`, `F.transfersTable()`, `F.feeBreakdown()` — data transform, no DOM, no fetch
- **Side-effect functions**: `api.*` (HTTP calls), `D.*` (DOM manipulation), `S.*` (session state)
- **Orchestration**: event handlers + screen switching — gather input → call API → render result

The client dashboard displays a read-only **Rates & Fees** card sourced from `GET /bank/rules` (public endpoint).

### Testing

Business function tests in `src/test/java/bank/domain/decisions/` need **zero mocks, zero Spring, zero database**:

```java
@Test void savingsCannotOverdraw() {
    var facts = new TransferDecision.Facts(
        Amount.ofEuros(100), Amount.ZERO, true,
        Amount.ofEuros(50), true, Amount.ofEuros(300),
        AccountType.SAVINGS);
    var result = TransferDecision.decide(facts);
    assertFalse(result.allowed());
    assertTrue(result.reason().contains("cannot overdraw"));
}
```

## Design Philosophy

The anti-OOP design principles governing this project are in [ARCHITECTURE.md](ARCHITECTURE.md). Read that document before adding or refactoring business logic. The architecture review checklist at the end of that document should be run before finishing any change.

Key guardrails:
- Pure functions never touch `LocalDate.now()`, `Math.random()`, repositories, or any I/O
- A `Facts` record must stay fixed-size — if it contains a collection, keep extracting the smaller feature the rule is really after
- Orchestration must not `if` on a pure function's output before deciding what to do — execute unconditionally
- Default rule values must never be hardcoded in services — rules are seeded in one place (`BankRuleStore`)
- JPA entities are package-private — the `domain` packages have zero `import` from `repository.entity`
