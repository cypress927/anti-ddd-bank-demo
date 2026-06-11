package bank.repository;

import bank.domain.facts.ClientFact;
import bank.repository.entity.ClientEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Side-effect function: persists client data via JPA → SQLite.
 * Prepares {@link ClientFact} projections for the business core.
 *
 * JPA entities ({@link ClientEntity}) are internal infrastructure detail.
 * Only {@link ClientFact} records cross the boundary into the domain.
 * Contains NO business judgment — only mechanical data preparation.
 */
@Component
@Transactional
public class ClientStore {

    private final ClientJpaRepo repo;

    public ClientStore(ClientJpaRepo repo) {
        this.repo = repo;
    }

    // ---- fact-preparation queries ----

    public Optional<ClientFact> findById(Long id) {
        return repo.findById(id).map(ClientEntity::toFact);
    }

    public Optional<ClientFact> findByUsername(String username) {
        return repo.findByUsername(username).map(ClientEntity::toFact);
    }

    /** Projects source data into a single boolean fact — the pure function needs nothing more. */
    public boolean existsByUsername(String username) {
        return repo.findByUsername(username).isPresent();
    }

    public List<ClientFact> findAll() {
        return repo.findAllByOrderByIdDesc().stream()
            .map(ClientEntity::toFact)
            .toList();
    }

    public List<ClientFact> findAllBornFrom(LocalDate fromBirth) {
        return repo.findAllByBirthDateGreaterThanEqualOrderByBirthDateDescIdDesc(fromBirth).stream()
            .map(ClientEntity::toFact)
            .toList();
    }

    // ---- side-effect commands ----

    public ClientFact save(String username, LocalDate birthDate) {
        var entity = new ClientEntity(username, birthDate);
        return repo.save(entity).toFact();
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public void deleteAll() {
        repo.deleteAll();
    }
}
