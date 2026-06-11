package bank.repository;

import bank.repository.entity.InterestLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for InterestLogEntity — PURE INFRASTRUCTURE.
 */
@Repository
interface InterestLogJpaRepo extends JpaRepository<InterestLogEntity, Long> {

    /** Sum of gross interest for a given account and year (year-to-date). */
    @Query("SELECT COALESCE(SUM(i.grossCents), 0) FROM InterestLogEntity i " +
           "WHERE i.accountNo = :accountNo AND i.year = :year")
    long sumGrossCentsByAccountNoAndYear(Long accountNo, int year);
}
