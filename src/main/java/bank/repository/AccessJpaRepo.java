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
 */
@Repository
interface AccessJpaRepo extends JpaRepository<AccessEntity, Long> {

    Optional<AccessEntity> findByClientIdAndAccountAccountNo(long clientId, Long accountNo);

    boolean existsByClientIdAndIsOwnerTrue(long clientId);

    List<AccessEntity> findAllByClientId(long clientId);

    /** Full accounts: balance >= minBalanceCents, ordered by balance desc then client id desc. */
    @Query("SELECT a FROM AccessEntity a JOIN FETCH a.client JOIN FETCH a.account " +
           "WHERE a.account.balanceCents >= :minCents " +
           "ORDER BY a.account.balanceCents DESC, a.client.id DESC")
    List<AccessEntity> findFullAccounts(Long minCents);

    @Modifying
    @Query("DELETE FROM AccessEntity a WHERE a.client.id = :clientId AND a.isOwner = false")
    int deleteNonOwnerByClientId(long clientId);
}
