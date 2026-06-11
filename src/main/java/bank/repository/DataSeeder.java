package bank.repository;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Infrastructure: seeds default bank rules after the application context is ready.
 * This runs after schema.sql has been executed, avoiding missing-table errors.
 */
@Component
public class DataSeeder {

    private final BankRuleStore bankRuleStore;

    public DataSeeder(BankRuleStore bankRuleStore) {
        this.bankRuleStore = bankRuleStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        bankRuleStore.seedDefaults();
    }
}
