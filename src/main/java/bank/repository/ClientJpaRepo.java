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
 *
 * NOTE: no existsBy* methods — they generate FETCH FIRST which SQLite doesn't support.
 * The Stores project existence facts mechanically from find* + isPresent().
 */
@Repository
interface ClientJpaRepo extends JpaRepository<ClientEntity, Long> {

    Optional<ClientEntity> findByUsername(String username);

    List<ClientEntity> findAllByOrderByIdDesc();

    List<ClientEntity> findAllByBirthDateGreaterThanEqualOrderByBirthDateDescIdDesc(LocalDate minDate);
}
