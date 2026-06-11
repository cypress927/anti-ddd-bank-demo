package bank.repository;

import bank.repository.entity.BankRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for BankRuleEntity — PURE INFRASTRUCTURE.
 */
@Repository
interface BankRuleJpaRepo extends JpaRepository<BankRuleEntity, String> {
}
