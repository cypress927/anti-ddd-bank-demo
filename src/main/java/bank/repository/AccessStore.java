package bank.repository;

import bank.domain.facts.AccessFact;
import bank.domain.facts.AccountNo;
import bank.domain.facts.Amount;
import bank.repository.entity.AccessEntity;
import bank.repository.entity.AccountEntity;
import bank.repository.entity.ClientEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Side-effect function: persists account-access links via JPA → SQLite.
 * Prepares {@link AccessFact} projections and boolean-flag facts
 * for the business core.
 *
 * Contains NO business judgment — only mechanical data preparation.
 */
@Component
@Transactional
public class AccessStore {

    private final AccessJpaRepo repo;
    private final ClientJpaRepo clientRepo;
    private final AccountJpaRepo accountRepo;

    public AccessStore(AccessJpaRepo repo, ClientJpaRepo clientRepo, AccountJpaRepo accountRepo) {
        this.repo = repo;
        this.clientRepo = clientRepo;
        this.accountRepo = accountRepo;
    }

    // ---- fact-preparation queries ----

    public Optional<AccessFact> findById(Long id) {
        return repo.findById(id).map(AccessEntity::toFact);
    }

    public Optional<AccessFact> findByClientAndAccount(long clientId, AccountNo accountNo) {
        return repo.findByClientIdAndAccountAccountNo(clientId, accountNo.number())
            .map(AccessEntity::toFact);
    }

    /** Projects "is owner" fact → single boolean for the pure function. */
    public boolean isOwner(long clientId, AccountNo accountNo) {
        return findByClientAndAccount(clientId, accountNo)
            .map(AccessFact::isOwner)
            .orElse(false);
    }

    /** Projects "owns any account" → single boolean for the pure function. */
    public boolean ownsAnyAccount(long clientId) {
        return repo.existsByClientIdAndIsOwnerTrue(clientId);
    }

    public List<AccessFact> findByClient(long clientId) {
        return repo.findAllByClientId(clientId).stream()
            .map(AccessEntity::toFact)
            .toList();
    }

    public List<AccessFact> findFullAccounts(Amount minBalance) {
        return repo.findFullAccounts(minBalance.cents()).stream()
            .map(AccessEntity::toFact)
            .toList();
    }

    // ---- side-effect commands ----

    public AccessFact save(long clientId, String clientUsername, boolean isOwner,
                            AccountNo accountNo, String accountName, Amount accountBalance) {
        // Load referenced entities (must exist — caller ensures this)
        var client = clientRepo.findById(clientId).orElseThrow();
        var account = accountRepo.findByAccountNo(accountNo.number()).orElseThrow();
        var entity = new AccessEntity(client, isOwner, account);
        return repo.save(entity).toFact();
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public int deleteNonOwnerAccesses(long clientId) {
        return repo.deleteNonOwnerByClientId(clientId);
    }

    public void deleteAll() {
        repo.deleteAll();
    }
}
