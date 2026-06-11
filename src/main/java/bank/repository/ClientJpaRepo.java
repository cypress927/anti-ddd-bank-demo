package bank.repository;

import bank.repository.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ClientEntity — PURE INFRASTRUCTURE.
 * Query method names are mechanically derived from storage fields.
 * No business judgment here.
 */
@Repository
interface ClientJpaRepo extends JpaRepository<ClientEntity, Long> {

    Optional<ClientEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    List<ClientEntity> findAllByOrderByIdDesc();

    /** Young clients: born at or after minDate, ordered by age ascending. */
    List<ClientEntity> findAllByBirthDateGreaterThanEqualOrderByBirthDateDescIdDesc(LocalDate minDate);
}
