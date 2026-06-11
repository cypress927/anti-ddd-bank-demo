package bank.repository;

import bank.repository.entity.TransferLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for TransferLogEntity — PURE INFRASTRUCTURE.
 */
@Repository
interface TransferLogJpaRepo extends JpaRepository<TransferLogEntity, Long> {

    /** Count transfers for a specific account in the current month. */
    @Query("SELECT COUNT(t) FROM TransferLogEntity t WHERE t.sourceAccountNo = :accountNo " +
           "AND t.createdAt >= :monthStart AND t.createdAt <= :monthEnd")
    long countBySourceAccountNoAndMonth(Long accountNo, LocalDate monthStart, LocalDate monthEnd);

    List<TransferLogEntity> findAllByOrderByCreatedAtDesc();

    /** Find transfers involving any of the given account numbers (source or destination). */
    @Query("SELECT t FROM TransferLogEntity t WHERE t.sourceAccountNo IN :accountNos " +
           "OR t.destinationAccountNo IN :accountNos ORDER BY t.createdAt DESC")
    List<TransferLogEntity> findByAccountNos(List<Long> accountNos);
}
