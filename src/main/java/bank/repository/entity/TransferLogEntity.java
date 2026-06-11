package bank.repository.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * JPA entity for the "transfer_logs" table — PURE INFRASTRUCTURE.
 * Records every transfer for fee statistics and monthly counting.
 */
@Entity
@Table(name = "transfer_logs")
public class TransferLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_account_no", nullable = false)
    private Long sourceAccountNo;

    @Column(name = "destination_account_no")
    private Long destinationAccountNo;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "fee_cents", nullable = false)
    private Long feeCents;

    @Column(name = "penalty_cents", nullable = false)
    private Long penaltyCents;

    @Column(name = "tax_cents", nullable = false)
    private Long taxCents;

    @Column(name = "is_internal", nullable = false)
    private boolean isInternal;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    protected TransferLogEntity() {}

    public TransferLogEntity(Long sourceAccountNo, Long destinationAccountNo,
                              Long amountCents, Long feeCents, Long penaltyCents,
                              Long taxCents, boolean isInternal, LocalDate createdAt) {
        this.sourceAccountNo = sourceAccountNo;
        this.destinationAccountNo = destinationAccountNo;
        this.amountCents = amountCents;
        this.feeCents = feeCents;
        this.penaltyCents = penaltyCents;
        this.taxCents = taxCents;
        this.isInternal = isInternal;
        this.createdAt = createdAt;
    }

    // ---- getters/setters ----

    public Long getId()                     { return id; }
    public void setId(Long id)              { this.id = id; }

    public Long getSourceAccountNo()        { return sourceAccountNo; }
    public void setSourceAccountNo(Long n)  { this.sourceAccountNo = n; }

    public Long getDestinationAccountNo()   { return destinationAccountNo; }
    public void setDestinationAccountNo(Long n) { this.destinationAccountNo = n; }

    public Long getAmountCents()            { return amountCents; }
    public void setAmountCents(Long c)      { this.amountCents = c; }

    public Long getFeeCents()               { return feeCents; }
    public void setFeeCents(Long c)         { this.feeCents = c; }

    public Long getPenaltyCents()           { return penaltyCents; }
    public void setPenaltyCents(Long c)     { this.penaltyCents = c; }

    public Long getTaxCents()               { return taxCents; }
    public void setTaxCents(Long c)         { this.taxCents = c; }

    public boolean isInternal()             { return isInternal; }
    public void setInternal(boolean b)      { this.isInternal = b; }

    public LocalDate getCreatedAt()          { return createdAt; }
    public void setCreatedAt(LocalDate d)   { this.createdAt = d; }
}
