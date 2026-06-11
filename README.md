<p align="center">
  <a href="#english">English</a> ·
  <a href="#chinese">中文</a>
</p>

---

<a id="english"></a>

# Anti-DDD Bank Demo

**A banking system redesigned with pure-function architecture — every complex business rule is fully testable with plain data, zero mocks, zero databases, zero frameworks.**

> **Design methodology**: This project follows the [Anti-OOP Design v2 Skill](https://github.com/cypress927/anti-oop-design-v2.SKILL) — a systematic approach to separating pure computation from side effects. See [ARCHITECTURE.md](ARCHITECTURE.md) for the full principles applied here.
>
> **Why this approach matters for AI-assisted development**: When working with AI coding agents, traditional OOP/DDD makes it easy for the agent to scatter business rules across Entity methods, Service classes, and inline validations — each change touching multiple files, making the system progressively harder to reason about and maintain. The pure-function architecture constrains both human and AI to a single pattern: *gather facts → decide → execute*. Every business rule lives in one place, as one static function with explicit inputs and outputs. This prevents over-engineering, keeps decisions auditable, and makes the codebase equally maintainable whether written by humans, agents, or both together.

## Why This Architecture

Traditional DDD projects embed business logic inside Entity methods that depend on injected Repositories. Testing requires starting a Spring container or mocking layers of objects:

```java
// Traditional OOP/DDD — business logic and infrastructure are entangled
account.transfer(destination, amount, accountRepository);
// Testing must mock accountRepository, or spin up a database
```

This project completely separates **computation** from **side effects**. Every business decision is a pure static function — it accepts fixed-size scalar facts as input and returns complete computed results as output. To test "savings accounts cannot overdraw," you write:

```java
// Pure-function architecture — zero dependencies, zero mocks
var facts = new TransferDecision.Facts(
    Amount.ofEuros(100), Amount.ZERO,  true,           // amount, fee, has-access
    Amount.ofEuros(50),  true, Amount.ofEuros(300),   // src-balance, dest-exists, dest-balance
    AccountType.SAVINGS);                              // account type
var result = TransferDecision.decide(facts);
assertFalse(result.allowed());  // Not enough funds in savings → rejected
assertTrue(result.reason().contains("cannot overdraw"));
```

**67 business tests. Zero Spring containers. Zero mocks. Zero databases. All execute in milliseconds.**

> Detailed design principles: [ARCHITECTURE.md](ARCHITECTURE.md). Project guide: [CLAUDE.md](CLAUDE.md).

---

## Features

### Account System
- **Checking (活期)**: No interest, overdraft allowed down to −1,000€
- **Savings (储蓄)**: Daily interest accrual, no overdraft, 6 free transfers per month

### Transfer Fees
| Fee Type | Rule |
|----------|------|
| Internal transfer | 0.50€ (flat) |
| External transfer | 1.00€ + amount × 0.05% (min 1€, max 50€) |
| Savings excess penalty | +2.00€ per transfer beyond 6/month |
| Large transfer tax | 0.1% on amount above 10,000€ (external only) |

### Interest
- Savings accounts earn daily interest: Net = Gross − Tax
- Annual tax exemption: 100€; amount above that taxed at 25%
- Banker can manually trigger interest accrual for any number of days

### Roles & Permissions

| Role | Capabilities |
|------|-------------|
| **Client** | Open accounts (Checking/Savings), deposit, transfer (with fee breakdown), view current rates, add account managers, view personal transfer history |
| **Banker** | Register/delete clients, find clients, view all transfers, modify 8 bank rules, trigger interest accrual |

Banker mutation endpoints are protected (`X-Banker-Key` header). Clients cannot access administrative functions.

### Configurable Rules (Banker)

| Rule Key | Default | Range | Description |
|----------|---------|-------|-------------|
| `interest.rate` | 2.0% | [0, 20] | Annual savings interest rate |
| `interest.tax.rate` | 25.0% | [0, 50] | Tax rate on interest income |
| `interest.tax.exemption` | 100€ | [0, 10000] | Annual tax exemption |
| `transfer.fee.internal` | 0.50€ | [0, 20] | Internal transfer flat fee |
| `transfer.fee.external.flat` | 1.00€ | [0, 20] | External transfer flat fee |
| `transfer.fee.external.percent` | 0.05% | [0, 5] | External transfer percentage fee |
| `transfer.tax.threshold` | 10000€ | [0, 100000] | Transaction tax threshold |
| `transfer.tax.rate` | 0.1% | [0, 1] | Transaction tax rate on excess |

**All rules take effect immediately** — no restart required. The Banker changes a value in the Web UI, the next transfer or interest accrual uses the new value. The Client dashboard displays a read-only **Rates & Fees** card sourced from `GET /bank/rules` (public), so clients always see the current rates.

**How these rules apply in practice:**

- **Internal transfer fee** — Every transfer between two accounts within the bank costs 0.50€ (default). This is the simplest case: uncheck "Internal" in the transfer form and the fee structure changes completely.

- **External transfer fee** — When "Internal" is unchecked, the transfer is treated as going to another bank. The fee becomes 1.00€ flat + 0.05% of the transfer amount, clamped between 1€ and 50€. A 1000€ external transfer costs 1.50€ (1.00 + 0.50). A 100,000€ external transfer hits the 50€ cap.

- **Savings excess penalty** — Savings accounts get 6 free transfers per month. From the 7th onward, each transfer adds a 2.00€ penalty. This applies regardless of internal/external — the penalty stacks on top. Checking accounts are never penalized.

- **Large transfer tax** — Only on external transfers. If the amount exceeds the threshold (default 10,000€), the portion above the threshold is taxed. A 15,000€ external transfer: excess = 5,000€, taxed at 0.1% = 5.00€. Below the threshold: zero tax.

- **Savings interest rate** — Savings accounts earn interest daily. A 10,000€ savings account at 2% annual rate earns roughly `10000 × 0.02 ÷ 365 × 30 ≈ 16.44€` over 30 days. Checking accounts earn nothing.

- **Interest tax** — Interest income is taxed, but the first 100€ per year is tax-free. Once your total interest for the year exceeds 100€, every additional euro of interest is taxed at 25%. A client who earns 150€ of interest in a year keeps 100€ tax-free and pays 25% × 50€ = 12.50€ tax, netting 137.50€.

- **Rule changes are instant** — The Banker edits a value in the UI → the next transfer or interest accrual uses it immediately. No restart. Clients see current rates in their dashboard's "Rates & Fees" card (read-only, from the public `GET /bank/rules` endpoint).

## Quick Start

### Prerequisites

- Java 17
- Maven 3.6+

### Build & Test

```bash
# Compile
mvn compile

# Run all tests (67 tests, zero dependencies)
mvn test

# Run a single test class
mvn test -Dtest=TransferDecisionTest

# Run a single test method
mvn test -Dtest=TransferDecisionTest#savingsCannotOverdraw
```

### Run the Application

```bash
# Delete old database (optional, for a clean state)
rm -f bank.db

# Start (Spring Boot, embedded Tomcat, SQLite)
mvn spring-boot:run

# Open in browser
# http://localhost:8080/
```

On first startup, the application automatically creates the SQLite database and seeds default bank rules.

## Usage Guide

### Web Interface

Open `http://localhost:8080/` to reach the login page:

**Banker Login**
- Username: `banker`
- Password: `banker123`
- Register clients, manage rules, trigger interest, view all transfers

**Client Login**
- A Banker must first register the client (e.g., `alice`)
- The client then logs in with that username
- Open accounts, deposit, transfer, view rates, view personal transfers

### Typical Workflow

```
Banker login → Register Client: alice, 1990-05-20

Client login(alice) → Open Account: "My Savings", SAVINGS
                    → Deposit: account #1, 5000€
                    → Open Account: "My Card", CHECKING
                    → Deposit: account #2, 1000€
                    → Transfer: from #2 to #1, 100€, Internal
                    → View rates, view transfer history

Banker login → Change interest rate to 3.5%
             → Accrue Interest: 30 days
             → View all transfers
```

### API Endpoints

#### Banker (Administration)

| Method | Path | Auth | Description |
|--------|------|:----:|-------------|
| `POST` | `/bank/login` | — | Authenticate, receive token |
| `POST` | `/bank/client` | 🔒 | Register a client |
| `DELETE` | `/bank/client/{username}` | 🔒 | Delete a client |
| `GET` | `/bank/client` | — | Find / list clients |
| `GET` | `/bank/rules` | — | View all rules (public) |
| `PUT` | `/bank/rules` | 🔒 | Update a rule |
| `POST` | `/bank/accrue-interest` | 🔒 | Trigger interest accrual |
| `GET` | `/bank/transfers` | — | View all transfers |

#### Client

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/client/account` | Open an account (CHECKING/SAVINGS) |
| `GET` | `/client/accounts` | View my accounts |
| `POST` | `/client/deposit` | Deposit funds |
| `POST` | `/client/transfer` | Transfer (returns fee breakdown) |
| `POST` | `/client/manager` | Add an account manager |
| `GET` | `/client/transfers` | View my transfer history |

All `/client/*` endpoints identify the user via the `X-Username` request header.
Banker endpoints marked 🔒 require `X-Banker-Key: banker123`.

## Architecture

```
 Transport (Controller)  ← HTTP parsing, auth, response formatting
          │
 Orchestration (Service) ← Load facts → call pure functions → execute effects
          │                        ↑
     ┌────┴────┐                   │
     │  Stores  │ → project facts ─┘
     │  (JPA)   │ ← persist results
     └─────────┘
          │
     Pure Functions    ← static methods, plain data in → plain data out
     (domain/decisions)   Zero I/O, zero framework, zero side effects
```

| Layer | Package | Responsibility | Framework dependency |
|-------|---------|----------------|:--:|
| Transport | `bank.transport` | Parse HTTP, delegate, format responses | Spring MVC |
| Orchestration | `bank.service` | Load facts → call pure functions → execute effects | Spring `@Service` |
| Pure Functions | `bank.domain.decisions` | Business rule computation | **None** |
| Facts | `bank.domain.facts` | Fixed-size data projections (records) | **None** |
| Stores | `bank.repository` | DB reads/writes, Entity → Fact mapping | Spring `@Component`, JPA |
| Entities | `bank.repository.entity` | JPA entities (infrastructure-internal) | JPA `@Entity` |

### 8 Pure Business Functions

| Function | Input Facts | Output |
|----------|------------|--------|
| `CreateClientDecision` | username, alreadyExists | allowed, reason |
| `DeleteClientDecision` | ownsAnyAccount | allowed, reason |
| `DepositDecision` | amount, currentBalance, destExists | allowed, reason, newBalance |
| `TransferDecision` | amount, totalFee, hasAccess, srcBalance, destExists, destBalance, accountType | allowed, reason, newSrcBalance, newDstBalance |
| `TransferFeeDecision` | amount, isInternal, accountType, monthlyCount, feeRules | baseFee, excessPenalty, transactionTax, totalFee |
| `AddAccountManagerDecision` | requesterIsOwner, managerHasAccess | allowed, reason |
| `InterestCalculationDecision` | balance, days, ytdInterest, rules | grossInterest, taxableInterest, taxAmount, netInterest |
| `BankRuleConfigDecision` | ruleKey, proposedValue | allowed, reason, effectiveValue |

All are testable with plain data — no I/O, no Spring, no database.

## Tech Stack

- Java 17, Maven 3.6+
- Spring Boot 3.0.13 (Web, Data JPA)
- Hibernate 6.1 + SQLite
- JUnit 5 (67 tests, zero mocks)
- Static HTML/CSS/JS (same pure/side-effect separation)

## Project Structure

```
src/main/java/bank/
  domain/
    decisions/     ← Pure business functions (8)
    facts/         ← Data projections: Amount, AccountNo, AccountFact, ClientFact, AccessFact, AccountType
  repository/
    entity/        ← JPA entities (infrastructure-internal)
    *Store.java    ← Side effects: DB reads/writes
    *JpaRepo.java  ← Spring Data JPA interfaces
  service/
    BankService.java   ← Banker orchestration
    ClientService.java ← Client orchestration
  transport/
    BankController.java   ← /bank/* REST endpoints
    ClientController.java ← /client/* REST endpoints
    Commands.java         ← DTOs

src/test/java/bank/domain/
  decisions/       ← Pure function tests (67, zero mocks)
  facts/

src/main/resources/
  static/          ← Web UI (pure/side-effect/orchestration separation)
  schema.sql       ← SQLite DDL
  application.properties
```

---

<p align="center"><a href="#">↑ Back to top</a></p>

---

<a id="chinese"></a>

# Anti-DDD Bank Demo（反 DDD 银行演示）

**一个用纯函数架构从头设计的银行系统 —— 所有复杂业务规则用纯数据即可完整测试，零 Mock、零数据库、零框架依赖。**

> **设计方法论**：本项目遵循 [Anti-OOP Design v2 Skill](https://github.com/cypress927/anti-oop-design-v2.SKILL) —— 一套系统性地将纯计算与副作用分离的设计方法。完整原则见 [ARCHITECTURE.md](ARCHITECTURE.md)。
>
> **为什么这对 AI 辅助开发很重要**：使用传统 OOP/DDD 时，AI 编程 agent 很容易将业务规则散落到 Entity 方法、Service 类、行内校验等各处——每次改动触及多个文件，系统越来越难以理解和维护。纯函数架构将人和 AI 约束在同一模式：*收集事实 → 判定 → 执行*。每条业务规则只在一个地方、作为一个静态函数存在，输入输出一目了然。这避免了过度设计，让决策可审计，无论是人写、agent 写、还是人机协作，代码库都同样可维护。

## 为什么这样设计

传统 DDD 项目把业务逻辑封装在 Entity 的方法里，通过依赖注入的 Repository 访问数据库。测试必须启动 Spring 容器，或者 Mock 一大堆对象：

```java
// 传统 OOP/DDD —— 业务逻辑和基础设施纠缠在一起
account.transfer(destination, amount, accountRepository);
// 测试必须 mock accountRepository，或者启动数据库
```

本项目把**计算**和**副作用**彻底分离。所有业务判断是纯静态函数——输入固定大小的标量事实，输出完整的计算结果。要测试"储蓄账户不允许透支"这条规则，只需：

```java
// 纯函数架构 —— 零依赖，零 Mock
var facts = new TransferDecision.Facts(
    Amount.ofEuros(100), Amount.ZERO,  true,           // 转账金额、手续费、有权限
    Amount.ofEuros(50),  true, Amount.ofEuros(300),   // 源余额、目标存在、目标余额
    AccountType.SAVINGS);                              // 账户类型
var result = TransferDecision.decide(facts);
assertFalse(result.allowed());  // 储蓄账户余额不足，拒绝转账
assertTrue(result.reason().contains("cannot overdraw"));
```

**67 个业务测试，零个 Spring 容器，零个 Mock，零个数据库。全部在毫秒级执行完毕。**

> 详细设计原则见 [ARCHITECTURE.md](ARCHITECTURE.md)，项目开发指南见 [CLAUDE.md](CLAUDE.md)。

---

## 功能概览

### 账户系统
- **活期账户（Checking）**：不计息，允许透支至 −1,000€
- **储蓄账户（Savings）**：按日计息，不允许透支，每月 6 次免费转账

### 转账费用
| 费用类型 | 规则 |
|----------|------|
| 行内转账费 | 0.50€（固定） |
| 行外转账费 | 1.00€ + 金额 × 0.05%（下限 1€，上限 50€） |
| 储蓄超额罚金 | 每月第 7 次起每次 +2.00€ |
| 大额交易税 | 行外转账超 10,000€ 部分征 0.1% |

### 存款利息
- 储蓄账户按日计息，净利息 = 毛利息 − 税额
- 年免税额 100€，超出部分按 25% 征税
- Banker 可手动触发任意天数的计息结算

### 角色与权限

| 角色 | 功能 |
|------|------|
| **Client（客户）** | 开设账户（活期/储蓄）、存款、转账（含费用明细）、查看当前费率、添加账户管理人、查看个人转账记录 |
| **Banker（行员）** | 注册/删除客户、查找客户、查看全部转账、修改 8 项银行规则、触发计息 |

Banker 的修改类端点受鉴权保护（`X-Banker-Key` 请求头），普通客户无法访问管理功能。

### 可配置规则（Banker 操作）

| 规则键 | 默认值 | 范围 | 说明 |
|--------|--------|------|------|
| `interest.rate` | 2.0% | [0, 20] | 储蓄年利率 |
| `interest.tax.rate` | 25.0% | [0, 50] | 利息税率 |
| `interest.tax.exemption` | 100€ | [0, 10000] | 年免税额 |
| `transfer.fee.internal` | 0.50€ | [0, 20] | 行内转账固定费 |
| `transfer.fee.external.flat` | 1.00€ | [0, 20] | 行外转账固定费 |
| `transfer.fee.external.percent` | 0.05% | [0, 5] | 行外转账比例费 |
| `transfer.tax.threshold` | 10000€ | [0, 100000] | 交易税起征点 |
| `transfer.tax.rate` | 0.1% | [0, 1] | 交易税率 |

**各规则在实践中的含义：**

- **行内转账费** — 行内两个账户之间每笔转账收取 0.50€（默认）。这是最简单的情况。取消勾选"Internal"复选框后，计费逻辑完全不同。

- **行外转账费** — 取消"Internal"时，转账被视为跨行。费用变为 1.00€ 固定费 + 金额 × 0.05%，下限 1€、上限 50€。一笔 1000€ 的行外转账费用为 1.50€（1.00 + 0.50）。10 万€ 的大额行外转账触及 50€ 上限。

- **储蓄超额罚金** — 储蓄账户每月前 6 次转账免费。第 7 次起每次额外加收 2.00€ 罚金，无论行内行外。活期账户无此限制。

- **大额交易税** — 仅行外转账触发。当金额超过起征点（默认 10,000€），超出部分按税率征税。一笔 15,000€ 的行外转账：超出 5,000€ × 0.1% = 5.00€ 交易税。低于起征点：零税。

- **储蓄年利率** — 储蓄账户按日计息。10,000€ 的储蓄账户按 2% 年利率，30 天约得利息 10000 × 0.02 ÷ 365 × 30 ≈ 16.44€。活期账户不产生利息。

- **利息税** — 利息收入需缴税，但每年前 100€ 免税。当年累计利息超 100€ 后，超出部分按 25% 征税。某客户年利息 150€：前 100€ 免税，后 50€ × 25% = 12.50€ 税，净得 137.50€。

- **规则即时生效** — Banker 在界面修改数值 → 下一次转账或计息立即使用新值，无需重启。客户面板"Rates & Fees"卡片展示当前费率（只读，来自公开的 `GET /bank/rules` 端点）。

## 快速开始

### 环境要求

- Java 17
- Maven 3.6+

### 构建与测试

```bash
# 编译
mvn compile

# 运行全部测试（67 个，纯业务逻辑，零依赖）
mvn test

# 运行单个测试类
mvn test -Dtest=TransferDecisionTest

# 运行单个测试方法
mvn test -Dtest=TransferDecisionTest#savingsCannotOverdraw
```

### 启动应用

```bash
# 删除旧数据库（可选，获取干净状态）
rm -f bank.db

# 启动（Spring Boot，内嵌 Tomcat，SQLite 存储）
mvn spring-boot:run

# 浏览器打开
# http://localhost:8080/
```

首次启动时，应用自动创建 SQLite 数据库并初始化默认银行规则。

## 使用指南

### Web 界面

打开 `http://localhost:8080/` 进入登录页：

**Banker（行员）登录**
- 用户名：`banker`
- 密码：`banker123`
- 可注册客户、管理规则、触发计息、查看全部转账

**Client（客户）登录**
- 先由 Banker 在面板中注册客户（例如 `alice`）
- 客户用已注册的用户名登录
- 可开设账户、存款、转账、查看费率、查看个人转账记录

### 典型操作流程

```
Banker 登录 → 注册客户：alice, 1990-05-20

Client 登录(alice) → 开设账户："My Savings", SAVINGS（储蓄）
                   → 存款：账户号 1, 金额 5000€
                   → 开设账户："My Card", CHECKING（活期）
                   → 存款：账户号 2, 金额 1000€
                   → 转账：从 #2 到 #1, 100€, 行内
                   → 查看费率、查看转账记录

Banker 登录 → 修改利率为 3.5%
           → 触发计息：30 天
           → 查看全部转账记录
```

### API 端点

#### Banker（管理端）

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|:--:|------|
| `POST` | `/bank/login` | — | 行员登录，获取 token |
| `POST` | `/bank/client` | 🔒 | 注册客户 |
| `DELETE` | `/bank/client/{username}` | 🔒 | 删除客户 |
| `GET` | `/bank/client` | — | 查找/列出客户 |
| `GET` | `/bank/rules` | — | 查看所有规则（公开） |
| `PUT` | `/bank/rules` | 🔒 | 修改规则 |
| `POST` | `/bank/accrue-interest` | 🔒 | 触发计息结算 |
| `GET` | `/bank/transfers` | — | 查看全部转账记录 |

#### Client（客户端）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/client/account` | 开设账户（可选 CHECKING 或 SAVINGS） |
| `GET` | `/client/accounts` | 查看我的账户 |
| `POST` | `/client/deposit` | 存款 |
| `POST` | `/client/transfer` | 转账（返回费用明细） |
| `POST` | `/client/manager` | 添加账户管理人 |
| `GET` | `/client/transfers` | 查看我的转账记录 |

所有 `/client/*` 端点通过 `X-Username` 请求头标识用户。🔒 标记的 Banker 端点需要 `X-Banker-Key: banker123` 请求头。

## 架构

```
 Transport（Controller）  ← HTTP 解析、鉴权、格式化响应
          │
 Orchestration（Service） ← 加载事实 → 调用纯函数 → 执行副作用
          │                        ↑
     ┌────┴────┐                   │
     │  Stores  │ → 投影为事实 ────┘
     │  (JPA)   │ ← 持久化结果
     └─────────┘
          │
     Pure Functions    ← static 方法，纯数据入 → 纯数据出
     (domain/decisions)   零 I/O、零框架、零副作用
```

| 层 | 包 | 职责 | 框架依赖 |
|----|-----|------|:--:|
| Transport | `bank.transport` | HTTP 请求解析、鉴权、响应格式化 | Spring MVC |
| Orchestration | `bank.service` | 加载事实 → 调纯函数 → 执行效果 | Spring `@Service` |
| Pure Functions | `bank.domain.decisions` | 业务规则计算 | **无** |
| Facts | `bank.domain.facts` | 固定大小的数据投影（record） | **无** |
| Stores | `bank.repository` | 数据库读写，Entity → Fact 映射 | Spring `@Component`、JPA |
| Entities | `bank.repository.entity` | JPA 实体（基础设施内部，不跨入 domain） | JPA `@Entity` |

### 8 个纯业务函数

| 函数 | 输入事实 | 输出 |
|------|----------|------|
| `CreateClientDecision` | username, alreadyExists | allowed, reason |
| `DeleteClientDecision` | ownsAnyAccount | allowed, reason |
| `DepositDecision` | amount, currentBalance, destExists | allowed, reason, newBalance |
| `TransferDecision` | amount, totalFee, hasAccess, srcBalance, destExists, destBalance, accountType | allowed, reason, newSrcBalance, newDstBalance |
| `TransferFeeDecision` | amount, isInternal, accountType, monthlyCount, feeRules | baseFee, excessPenalty, transactionTax, totalFee |
| `AddAccountManagerDecision` | requesterIsOwner, managerHasAccess | allowed, reason |
| `InterestCalculationDecision` | balance, days, ytdInterest, rules | grossInterest, taxableInterest, taxAmount, netInterest |
| `BankRuleConfigDecision` | ruleKey, proposedValue | allowed, reason, effectiveValue |

全部可在测试中用纯数据构造，无任何 I/O、无 Spring、无数据库。

## 技术栈

- Java 17，Maven 3.6+
- Spring Boot 3.0.13（Web，Data JPA）
- Hibernate 6.1 + SQLite
- JUnit 5（67 个测试，零 Mock）
- 静态 HTML/CSS/JS（遵循相同的纯/副作用分离原则）

## 项目结构

```
src/main/java/bank/
  domain/
    decisions/     ← 纯业务函数（8 个）
    facts/         ← 数据投影：Amount、AccountNo、AccountFact、ClientFact、AccessFact、AccountType
  repository/
    entity/        ← JPA 实体（基础设施内部）
    *Store.java    ← 副作用：数据库读写
    *JpaRepo.java  ← Spring Data JPA 接口
  service/
    BankService.java   ← Banker 编排放
    ClientService.java ← Client 编排放
  transport/
    BankController.java   ← /bank/* REST 端点
    ClientController.java ← /client/* REST 端点
    Commands.java         ← DTO

src/test/java/bank/domain/
  decisions/       ← 纯函数测试（67 个，零 Mock）
  facts/

src/main/resources/
  static/          ← Web UI（纯函数/副作用/编排分离）
  schema.sql       ← SQLite 建表 DDL
  application.properties
```

---

<p align="center">
  <a href="#">↑ 回到顶部</a> ·
  <a href="#english">English</a> ·
  <a href="#chinese">中文</a>
</p>
