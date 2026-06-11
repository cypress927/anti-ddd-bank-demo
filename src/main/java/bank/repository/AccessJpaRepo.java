package bank.repository;

import bank.repository.entity.AccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for AccessEntity — PURE INFRASTRUCTURE.
 *
 * NOTE: no existsBy* methods — they generate FETCH FIRST which SQLite doesn't support.
 * Use countBy* or find* + isPresent() instead.
 */
@Repository
interface AccessJpaRepo extends JpaRepository<AccessEntity, Long> {

    Optional<AccessEntity> findByClientIdAndAccountAccountNo(long clientId, Long accountNo);

    /** COUNT avoids FETCH FIRST — returns a scalar directly. */
    long countByClientIdAndIsOwnerTrue(long clientId);

    List<AccessEntity> findAllByClientId(long clientId);

    @Query("SELECT a FROM AccessEntity a JOIN FETCH a.client JOIN FETCH a.account " +
           "WHERE a.account.balanceCents >= :minCents " +
           "ORDER BY a.account.balanceCents DESC, a.client.id DESC")
    List<AccessEntity> findFullAccounts(Long minCents);

    @Modifying
    @Query("DELETE FROM AccessEntity a WHERE a.client.id = :clientId AND a.isOwner = false")
    int deleteNonOwnerByClientId(long clientId);
}
