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

# Run all tests
$MVN test

# Run a single test class
$MVN test -Dtest=TransferDecisionTest

# Run a single test method
$MVN test -Dtest=TransferDecisionTest#allowValidTransfer

# Start the application (remove bank.db first for a clean state)
rm -f bank.db && $MVN spring-boot:run

# The app serves at http://localhost:8080/
# Static web UI at http://localhost:8080/ (auto-served from src/main/resources/static/)
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

### Layers (top to bottom)

| Layer | Package | Role | Framework dependency |
|-------|---------|------|---------------------|
| **Transport** | `bank.transport` | Parse HTTP, delegate, format responses | Spring MVC `@RestController` |
| **Orchestration** | `bank.service` | Load facts → call pure function → execute effects | Spring `@Service`, `@Transactional` |
| **Pure decisions** | `bank.domain.decisions` | Business rules as `static` pure functions | **None** |
| **Fact records** | `bank.domain.facts` | Plain Java records for business data | **None** |
| **Stores** | `bank.repository` | Side-effect: project DB rows into facts | Spring `@Component`, JPA |
| **JPA entities** | `bank.repository.entity` | Infrastructure-only: map tables to rows. **Never cross into domain** | JPA `@Entity` |

### Pure business decisions (5 operations)

Each decision is a `final class` with:
- A `record Facts(...)` — fixed-size scalar/boolean inputs
- A `record Result(...)` — complete computed output
- A `static Result decide(Facts)` method — the pure computation

| Decision | Facts shape | Business rules |
|----------|------------|----------------|
| `CreateClientDecision` | `(username: String, alreadyExists: boolean)` | Username pattern `[a-z_A-Z][a-z_A-Z0-9]{0,30}`, uniqueness |
| `DeleteClientDecision` | `(ownsAnyAccount: boolean)` | Cannot delete if owns accounts |
| `DepositDecision` | `(amount, currentBalance, destinationExists)` | Amount > 0, computes new balance |
| `TransferDecision` | `(amount, hasAccessRight, sourceBalance, destExists, destBalance)` | Amount > 0, access check, minimum balance -1000€, computes both new balances |
| `AddAccountManagerDecision` | `(requesterIsOwner, managerAlreadyHasAccess)` | Only owner can add, no duplicates |

### Stores and JPA

Stores are the **only** classes that touch JPA entities. They:
1. Query via Spring Data JPA repository interfaces (`*JpaRepo`)
2. Map `Entity → Fact` mechanically via `toFact()`
3. Never contain business judgment

**SQLite caveats**:
- `existsBy*` derived queries are avoided — they generate `FETCH FIRST n ROWS ONLY` which SQLite doesn't support. Use `findBy*().isPresent()` or `countBy*() > 0` in Stores instead.
- Custom `SQLiteDialect` in `bank.repository.entity` provides identity-column support only.
- DDL is manual `schema.sql` (not Hibernate auto-DDL) because SQLite doesn't support `ALTER TABLE DROP CONSTRAINT`.

### Web UI

Static files in `src/main/resources/static/` follow the same pure/side-effect separation:
- **Pure functions** (in `app.js`): `formatBalance()`, `clientToRow()`, `clientsToTable()` — data transform, no DOM, no fetch
- **Side-effect functions**: `api.*` (HTTP calls), `dom.*` (DOM manipulation)
- **Orchestration**: event handlers — gather input → call API → render result

### Client authentication

The `ClientController` accepts an `X-Username` header (no Spring Security required for demo). The web UI reads the username from the top input field and sends it as `X-Username` on every `/client/*` request.

### Testing

Business function tests in `src/test/java/bank/domain/decisions/` need **zero mocks, zero Spring, zero database**:

```java
@Test void rejectZeroAmount() {
    var facts = new TransferDecision.Facts(
        Amount.ZERO, true, Amount.ofEuros(500), true, Amount.ofEuros(300));
    var result = TransferDecision.decide(facts);
    assertFalse(result.allowed());
}
```

## Design Philosophy

The anti-OOP design principles governing this project are in [ARCHITECTURE.md](ARCHITECTURE.md). Read that document before adding or refactoring business logic. The architecture review checklist at the end of that document should be run before finishing any change.
