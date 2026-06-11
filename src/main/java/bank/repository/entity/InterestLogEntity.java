package bank.repository.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * JPA entity for the "interest_logs" table — PURE INFRASTRUCTURE.
 * Records each interest accrual for year-to-date tracking.
 */
@Entity
@Table(name = "interest_logs")
public class InterestLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_no", nullable = false)
    private Long accountNo;

    @Column(name = "balance_cents", nullable = false)
    private Long balanceCents;

    @Column(nullable = false)
    private Long days;

    @Column(name = "gross_cents", nullable = false)
    private Long grossCents;

    @Column(name = "taxable_cents", nullable = false)
    private Long taxableCents;

    @Column(name = "tax_cents", nullable = false)
    private Long taxCents;

    @Column(name = "net_cents", nullable = false)
    private Long netCents;

    @Column(nullable = false)
    private int year;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    protected InterestLogEntity() {}

    public InterestLogEntity(Long accountNo, Long balanceCents, Long days,
                              Long grossCents, Long taxableCents, Long taxCents,
                              Long netCents, int year, LocalDate createdAt) {
        this.accountNo = accountNo;
        this.balanceCents = balanceCents;
        this.days = days;
        this.grossCents = grossCents;
        this.taxableCents = taxableCents;
        this.taxCents = taxCents;
        this.netCents = netCents;
        this.year = year;
        this.createdAt = createdAt;
    }

    // ---- getters/setters ----

    public Long getId()                 { return id; }
    public void setId(Long id)          { this.id = id; }
    public Long getAccountNo()          { return accountNo; }
    public void setAccountNo(Long n)    { this.accountNo = n; }
    public Long getBalanceCents()       { return balanceCents; }
    public void setBalanceCents(Long c) { this.balanceCents = c; }
    public Long getDays()               { return days; }
    public void setDays(Long d)         { this.days = d; }
    public Long getGrossCents()         { return grossCents; }
    public void setGrossCents(Long c)   { this.grossCents = c; }
    public Long getTaxableCents()       { return taxableCents; }
    public void setTaxableCents(Long c) { this.taxableCents = c; }
    public Long getTaxCents()           { return taxCents; }
    public void setTaxCents(Long c)     { this.taxCents = c; }
    public Long getNetCents()           { return netCents; }
    public void setNetCents(Long c)     { this.netCents = c; }
    public int getYear()                { return year; }
    public void setYear(int y)          { this.year = y; }
    public LocalDate getCreatedAt()     { return createdAt; }
    public void setCreatedAt(LocalDate d){ this.createdAt = d; }
}
