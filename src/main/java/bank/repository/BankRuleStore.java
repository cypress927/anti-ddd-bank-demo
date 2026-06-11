package bank.repository;

import bank.repository.entity.BankRuleEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Side-effect function: reads and writes bank rules via JPA → SQLite.
 * Seeding is handled by {@link DataSeeder} after the context is ready.
 * Contains NO business judgment.
 */
@Component
@Transactional
public class BankRuleStore {

    private final BankRuleJpaRepo repo;

    public BankRuleStore(BankRuleJpaRepo repo) {
        this.repo = repo;
    }

    /** Seed default rules if the table is empty. Called by DataSeeder after context init. */
    public void seedDefaults() {
        if (repo.count() > 0) return;
        repo.save(new BankRuleEntity("interest.rate",                 2.0,  "Annual interest rate (%) for savings accounts"));
        repo.save(new BankRuleEntity("interest.tax.rate",            25.0,  "Tax rate on interest income (%)"));
        repo.save(new BankRuleEntity("interest.tax.exemption",      100.0,  "Yearly tax exemption on interest (EUR)"));
        repo.save(new BankRuleEntity("transfer.fee.internal",         0.50, "Flat fee for internal transfers (EUR)"));
        repo.save(new BankRuleEntity("transfer.fee.external.flat",    1.00, "Flat fee for external transfers (EUR)"));
        repo.save(new BankRuleEntity("transfer.fee.external.percent", 0.05, "Percentage fee for external transfers (%)"));
        repo.save(new BankRuleEntity("transfer.tax.threshold",    10000.0,  "Transaction tax trigger threshold (EUR)"));
        repo.save(new BankRuleEntity("transfer.tax.rate",             0.1,  "Transaction tax rate on excess above threshold (%)"));
    }

    /** Returns all rules as a Map<String,Double> for pure function consumption. */
    public Map<String, Double> getAll() {
        var map = new HashMap<String, Double>();
        for (var e : repo.findAll()) {
            map.put(e.getRuleKey(), e.getRuleValue());
        }
        return Collections.unmodifiableMap(map);
    }

    /** Single rule lookup. */
    public Optional<Double> get(String ruleKey) {
        return repo.findById(ruleKey).map(BankRuleEntity::getRuleValue);
    }

    /** Update a rule value — pure function already validated the new value. */
    public void update(String ruleKey, double newValue) {
        var entity = repo.findById(ruleKey)
            .orElseThrow(() -> new NoSuchElementException("Rule not found: " + ruleKey));
        entity.setRuleValue(newValue);
        repo.save(entity);
    }

    /** List all rules with descriptions for the UI. */
    public List<Map<String, Object>> listAll() {
        return repo.findAll().stream()
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("key", e.getRuleKey());
                m.put("value", e.getRuleValue());
                m.put("description", e.getDescription());
                return m;
            })
            .toList();
    }
}
