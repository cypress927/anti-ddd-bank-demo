package bank.repository;

import bank.domain.facts.AccountFact;
import bank.domain.facts.AccountNo;
import bank.domain.facts.Amount;
import bank.repository.entity.AccountEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Side-effect function: persists account data via JPA → SQLite.
 * Prepares {@link AccountFact} projections for the business core.
 * Contains NO business judgment — only mechanical data preparation.
 */
@Component
@Transactional
public class AccountStore {

    private final AccountJpaRepo repo;

    public AccountStore(AccountJpaRepo repo) {
        this.repo = repo;
    }

    // ---- fact-preparation queries ----

    public Optional<AccountFact> findByNo(AccountNo accountNo) {
        return repo.findByAccountNo(accountNo.number()).map(AccountEntity::toFact);
    }

    public boolean existsByNo(AccountNo accountNo) {
        return repo.findByAccountNo(accountNo.number()).isPresent();
    }

    // ---- side-effect commands ----

    public AccountFact save(String name) {
        var entity = new AccountEntity(name);
        return repo.save(entity).toFact();
    }

    /** Update only the balance — pure computation already decided the new value.
     *  Does a fresh SQL SELECT to return updated state. */
    public AccountFact updateBalance(AccountNo accountNo, Amount newBalance) {
        int rows = repo.updateBalance(accountNo.number(), newBalance.cents());
        if (rows == 0) {
            throw new NoSuchElementException("Account not found: " + accountNo);
        }
        return repo.findByAccountNo(accountNo.number())
            .map(AccountEntity::toFact)
            .orElseThrow();
    }

    /** Update balance when caller already holds the AccountFact — avoids extra SQL SELECT. */
    public AccountFact updateBalance(AccountFact existing, Amount newBalance) {
        repo.updateBalance(existing.accountNo().number(), newBalance.cents());
        return existing.withBalance(newBalance);
    }

    public void deleteAll() {
        repo.deleteAll();
    }

    /** Rich accounts: balance >= minBalance, ordered by balance desc. */
    public List<Map.Entry<Long, AccountFact>> findRichAccounts(Amount minBalance) {
        // We don't use this directly anymore — see AccessStore.findFullAccounts
        var results = new ArrayList<Map.Entry<Long, AccountFact>>();
        var all = repo.findAll();
        all.stream()
            .filter(a -> a.getBalanceCents() >= minBalance.cents())
            .sorted((a, b) -> {
                int cmp = Long.compare(b.getBalanceCents(), a.getBalanceCents());
                if (cmp != 0) return cmp;
                return Long.compare(b.getAccountNo(), a.getAccountNo());
            })
            .forEach(a -> results.add(Map.entry(a.getAccountNo(), a.toFact())));
        return results;
    }
}
