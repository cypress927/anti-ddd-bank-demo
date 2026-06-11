package bank.repository.entity;

import bank.domain.facts.AccountFact;
import bank.domain.facts.AccountNo;
import bank.domain.facts.AccountType;
import bank.domain.facts.Amount;

import jakarta.persistence.*;

/**
 * JPA entity for the "accounts" table — PURE INFRASTRUCTURE.
 * Lives only in repository layer. Never leaks into domain.
 * The toFact() method mechanically translates to a domain fact record.
 */
@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountNo;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long balanceCents;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    // ---- JPA-required no-arg constructor ----
    protected AccountEntity() {}

    public AccountEntity(String name, AccountType accountType) {
        this.name = name;
        this.balanceCents = 0L;
        this.accountType = accountType;
    }

    // ---- mechanical translation to/from domain facts ----

    public AccountFact toFact() {
        return new AccountFact(
            new AccountNo(this.accountNo),
            this.name,
            new Amount(this.balanceCents),
            this.accountType
        );
    }

    // ---- getters/setters (mechanical) ----

    public Long getAccountNo()                { return accountNo; }
    public void setAccountNo(Long no)         { this.accountNo = no; }

    public String getName()                   { return name; }
    public void setName(String name)          { this.name = name; }

    public Long getBalanceCents()             { return balanceCents; }
    public void setBalanceCents(Long c)       { this.balanceCents = c; }

    public AccountType getAccountType()       { return accountType; }
    public void setAccountType(AccountType t) { this.accountType = t; }
}
