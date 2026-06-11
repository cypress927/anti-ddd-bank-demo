package bank.repository.entity;

import bank.domain.facts.AccessFact;
import bank.domain.facts.AccountNo;
import bank.domain.facts.Amount;

import jakarta.persistence.*;

/**
 * JPA entity for the "account_accesses" table — PURE INFRASTRUCTURE.
 * Many-to-many link between Client and Account with an ownership flag.
 * toFact() mechanically denormalizes into an AccessFact record.
 */
@Entity
@Table(name = "account_accesses",
       uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "account_no"}))
public class AccessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @Column(name = "is_owner", nullable = false)
    private boolean isOwner;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_no", nullable = false)
    private AccountEntity account;

    protected AccessEntity() {}

    public AccessEntity(ClientEntity client, boolean isOwner, AccountEntity account) {
        this.client = client;
        this.isOwner = isOwner;
        this.account = account;
    }

    /** Mechanical translation — denormalizes client/account fields into the fact record. */
    public AccessFact toFact() {
        return new AccessFact(
            this.id,
            this.client.getId(),
            this.client.getUsername(),
            this.isOwner,
            new AccountNo(this.account.getAccountNo()),
            this.account.getName(),
            new Amount(this.account.getBalanceCents())
        );
    }

    // ---- getters/setters ----

    public Long getId()                 { return id; }
    public void setId(Long id)         { this.id = id; }

    public ClientEntity getClient()     { return client; }
    public void setClient(ClientEntity c) { this.client = c; }

    public boolean isOwner()            { return isOwner; }
    public void setOwner(boolean o)     { isOwner = o; }

    public AccountEntity getAccount()   { return account; }
    public void setAccount(AccountEntity a) { this.account = a; }
}
