package bank.service;

import bank.domain.decisions.BankRuleConfigDecision;
import bank.domain.decisions.CreateClientDecision;
import bank.domain.decisions.DeleteClientDecision;
import bank.domain.decisions.InterestCalculationDecision;
import bank.domain.facts.AccountType;
import bank.domain.facts.Amount;
import bank.domain.facts.ClientFact;
import bank.repository.AccessStore;
import bank.repository.AccountStore;
import bank.repository.BankRuleStore;
import bank.repository.ClientStore;
import bank.repository.InterestLogStore;
import bank.repository.TransferLogStore;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Thin orchestration for bank-clerk operations.
 * Responsibilities:
 *   1. Load facts from side-effect stores.
 *   2. Call pure business functions.
 *   3. Execute side effects from the returned result.
 *
 * Contains NO business judgment — all decisions live in pure functions.
 */
@Service
public class BankService {

    private final ClientStore clientStore;
    private final AccountStore accountStore;
    private final AccessStore accessStore;
    private final BankRuleStore bankRuleStore;
    private final TransferLogStore transferLogStore;
    private final InterestLogStore interestLogStore;

    public BankService(ClientStore clientStore, AccountStore accountStore,
                       AccessStore accessStore, BankRuleStore bankRuleStore,
                       TransferLogStore transferLogStore, InterestLogStore interestLogStore) {
        this.clientStore = clientStore;
        this.accountStore = accountStore;
        this.accessStore = accessStore;
        this.bankRuleStore = bankRuleStore;
        this.transferLogStore = transferLogStore;
        this.interestLogStore = interestLogStore;
    }

    // ---- createClient ----

    public ClientFact createClient(String username, LocalDate birthDate) {
        var facts = new CreateClientDecision.Facts(
            username, clientStore.existsByUsername(username));
        var result = CreateClientDecision.decide(facts);
        if (!result.allowed()) {
            throw new BusinessException(result.reason());
        }
        return clientStore.save(username, birthDate);
    }

    // ---- deleteClient ----

    public void deleteClient(String username) {
        var client = clientStore.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Client not found: " + username));
        var facts = new DeleteClientDecision.Facts(
            accessStore.ownsAnyAccount(client.id()));
        var result = DeleteClientDecision.decide(facts);
        if (!result.allowed()) {
            throw new BusinessException(result.reason());
        }
        accessStore.deleteNonOwnerAccesses(client.id());
        clientStore.delete(client.id());
    }

    // ---- findClient ----

    public ClientFact findClient(String username) {
        return clientStore.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Client not found: " + username));
    }

    // ---- findAllClients ----

    public List<ClientFact> findAllClients() {
        return clientStore.findAll();
    }

    // ---- findYoungClients ----

    public List<ClientFact> findYoungClients(LocalDate fromBirth) {
        return clientStore.findAllBornFrom(fromBirth);
    }

    // ---- findRichClients ----

    public List<ClientFact> findRichClients(Amount minBalance) {
        var richAccesses = accessStore.findFullAccounts(minBalance);
        return richAccesses.stream()
            .map(a -> clientStore.findById(a.clientId()))
            .flatMap(Optional::stream)
            .distinct()
            .toList();
    }

    // ---- bank rules ----

    /** Get all bank rules for display. */
    public List<Map<String, Object>> getAllRules() {
        return bankRuleStore.listAll();
    }

    /** Update a bank rule — pure function validates, store persists. */
    public void updateRule(String ruleKey, double newValue) {
        var facts = new BankRuleConfigDecision.Facts(ruleKey, newValue);
        var result = BankRuleConfigDecision.decide(facts);
        if (!result.allowed()) {
            throw new BusinessException(result.reason());
        }
        bankRuleStore.update(ruleKey, result.effectiveValue());
    }

    // ---- interest accrual ----

    /**
     * Accrue interest for all savings accounts over the given number of days.
     * Returns a summary of interest accrued.
     */
    public List<Map<String, Object>> accrueInterest(long days) {
        var rulesMap = bankRuleStore.getAll();
        // Rules are always seeded — missing key is a data integrity error
        var interestRules = new InterestCalculationDecision.InterestRules(
            requireRule(rulesMap, "interest.rate"),
            requireRule(rulesMap, "interest.tax.rate"),
            requireRule(rulesMap, "interest.tax.exemption")
        );
        int currentYear = LocalDate.now().getYear();

        List<Map<String, Object>> results = new ArrayList<>();
        for (var account : accountStore.findAllSavingsAccounts()) {
            Amount ytdInterest = interestLogStore.yearToDateInterest(
                account.accountNo(), currentYear);

            var facts = new InterestCalculationDecision.Facts(
                account.balance(), days, ytdInterest, interestRules);
            var result = InterestCalculationDecision.decide(facts);

            // Execute effect unconditionally — the pure function already decided the value
            var updated = accountStore.updateBalance(
                account, account.balance().plus(result.netInterest()));
            // Always record (even if zero — for audit trail)
            interestLogStore.record(account.accountNo(), account.balance(),
                days, result, currentYear);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("accountNo", updated.accountNo().number());
            entry.put("accountName", updated.name());
            entry.put("balance", updated.balance().toDouble());
            entry.put("grossInterest", result.grossInterest().toDouble());
            entry.put("taxableInterest", result.taxableInterest().toDouble());
            entry.put("taxAmount", result.taxAmount().toDouble());
            entry.put("netInterest", result.netInterest().toDouble());
            results.add(entry);
        }
        return results;
    }

    // ---- transfer log ----

    public List<Map<String, Object>> recentTransfers(int limit) {
        return transferLogStore.recentTransfers(limit);
    }

    /** Extracts a rule value from the map — throws if key is absent (data integrity error). */
    private static double requireRule(Map<String, Double> rules, String key) {
        var value = rules.get(key);
        if (value == null) {
            throw new IllegalStateException(
                "Bank rule '%s' is not seeded. Check BankRuleStore.".formatted(key));
        }
        return value;
    }
}
