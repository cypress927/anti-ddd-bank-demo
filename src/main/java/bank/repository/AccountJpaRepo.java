package bank.repository;

import bank.repository.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for AccountEntity — PURE INFRASTRUCTURE.
 */
@Repository
interface AccountJpaRepo extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByAccountNo(Long accountNo);

    @Modifying
    @Query("UPDATE AccountEntity a SET a.balanceCents = :newCents WHERE a.accountNo = :accountNo")
    int updateBalance(Long accountNo, Long newCents);
}
