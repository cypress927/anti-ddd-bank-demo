package bank.repository;

import bank.domain.facts.AccountNo;
import bank.domain.facts.Amount;
import bank.domain.decisions.TransferFeeDecision;
import bank.repository.entity.TransferLogEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Side-effect function: persists transfer logs via JPA → SQLite.
 * Provides monthly transfer counts for fee decisions.
 * Contains NO business judgment.
 */
@Component
@Transactional
public class TransferLogStore {

    private final TransferLogJpaRepo repo;

    public TransferLogStore(TransferLogJpaRepo repo) {
        this.repo = repo;
    }

    /** Count transfers for a source account in the current calendar month. */
    public int countThisMonth(AccountNo sourceAccountNo) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        return (int) repo.countBySourceAccountNoAndMonth(
            sourceAccountNo.number(), monthStart, monthEnd);
    }

    /** Record a completed transfer with its fee breakdown. */
    public void record(AccountNo source, AccountNo destination, Amount amount,
                        TransferFeeDecision.Result feeResult, boolean isInternal) {
        var entity = new TransferLogEntity(
            source.number(),
            destination.number(),
            amount.cents(),
            feeResult.baseFee().cents(),
            feeResult.excessPenalty().cents(),
            feeResult.transactionTax().cents(),
            isInternal,
            LocalDate.now()
        );
        repo.save(entity);
    }

    /** Find transfers involving any of the given accounts. */
    public List<Map<String, Object>> findByAccounts(List<AccountNo> accountNos, int limit) {
        if (accountNos.isEmpty()) return List.of();
        var nos = accountNos.stream().map(AccountNo::number).toList();
        return repo.findByAccountNos(nos).stream()
            .limit(limit)
            .map(this::toMap)
            .toList();
    }

    /** List recent transfers for display. */
    public List<Map<String, Object>> recentTransfers(int limit) {
        return repo.findAllByOrderByCreatedAtDesc().stream()
            .limit(limit)
            .map(this::toMap)
            .toList();
    }

    /** Mechanical: entity → map. */
    private Map<String, Object> toMap(TransferLogEntity e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("source", e.getSourceAccountNo());
        m.put("destination", e.getDestinationAccountNo());
        m.put("amount", e.getAmountCents() / 100.0);
        m.put("fee", e.getFeeCents() / 100.0);
        m.put("penalty", e.getPenaltyCents() / 100.0);
        m.put("tax", e.getTaxCents() / 100.0);
        m.put("isInternal", e.isInternal());
        m.put("date", e.getCreatedAt().toString());
        return m;
    }
}
