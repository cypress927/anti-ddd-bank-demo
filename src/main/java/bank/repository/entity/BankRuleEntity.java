package bank.repository.entity;

import jakarta.persistence.*;

/**
 * JPA entity for the "bank_rules" table — PURE INFRASTRUCTURE.
 * Stores admin-configurable bank rules as simple key-value pairs.
 */
@Entity
@Table(name = "bank_rules")
public class BankRuleEntity {

    @Id
    @Column(name = "rule_key")
    private String ruleKey;

    @Column(name = "rule_value", nullable = false)
    private double ruleValue;

    @Column
    private String description;

    protected BankRuleEntity() {}

    public BankRuleEntity(String ruleKey, double ruleValue, String description) {
        this.ruleKey = ruleKey;
        this.ruleValue = ruleValue;
        this.description = description;
    }

    public String getRuleKey()          { return ruleKey; }
    public void setRuleKey(String k)    { this.ruleKey = k; }

    public double getRuleValue()        { return ruleValue; }
    public void setRuleValue(double v)  { this.ruleValue = v; }

    public String getDescription()      { return description; }
    public void setDescription(String d){ this.description = d; }
}
