package bank.repository;

import bank.domain.facts.AccountNo;
import bank.domain.facts.Amount;
import bank.domain.decisions.InterestCalculationDecision;
import bank.repository.entity.InterestLogEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Side-effect function: persists interest logs via JPA → SQLite.
 * Provides year-to-date interest aggregation.
 * Contains NO business judgment.
 */
@Component
@Transactional
public class InterestLogStore {

    private final InterestLogJpaRepo repo;

    public InterestLogStore(InterestLogJpaRepo repo) {
        this.repo = repo;
    }

    /** Sum of gross interest for an account in the given year (year-to-date). */
    public Amount yearToDateInterest(AccountNo accountNo, int year) {
        long sumCents = repo.sumGrossCentsByAccountNoAndYear(accountNo.number(), year);
        return new Amount(sumCents);
    }

    /** Record an interest accrual. */
    public void record(AccountNo accountNo, Amount balance, long days,
                        InterestCalculationDecision.Result result, int year) {
        var entity = new InterestLogEntity(
            accountNo.number(),
            balance.cents(),
            days,
            result.grossInterest().cents(),
            result.taxableInterest().cents(),
            result.taxAmount().cents(),
            result.netInterest().cents(),
            year,
            LocalDate.now()
        );
        repo.save(entity);
    }

    /** List interest log entries for an account. */
    public List<Map<String, Object>> findByAccount(AccountNo accountNo, int limit) {
        return repo.findAll().stream()
            .filter(e -> e.getAccountNo().equals(accountNo.number()))
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(limit)
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gross", e.getGrossCents() / 100.0);
                m.put("taxable", e.getTaxableCents() / 100.0);
                m.put("tax", e.getTaxCents() / 100.0);
                m.put("net", e.getNetCents() / 100.0);
                m.put("year", e.getYear());
                m.put("date", e.getCreatedAt().toString());
                return m;
            })
            .toList();
    }
}
