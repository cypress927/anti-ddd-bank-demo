package bank.service;

import bank.domain.decisions.AddAccountManagerDecision;
import bank.domain.decisions.DepositDecision;
import bank.domain.decisions.TransferDecision;
import bank.domain.facts.AccountFact;
import bank.domain.facts.AccountNo;
import bank.domain.facts.AccountType;
import bank.domain.facts.AccessFact;
import bank.domain.facts.Amount;
import bank.domain.facts.ClientFact;
import bank.repository.AccessStore;
import bank.repository.AccountStore;
import bank.repository.BankRuleStore;
import bank.repository.ClientStore;
import bank.repository.TransferLogStore;

import static bank.repository.BankRuleStore.requireRule;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Thin orchestration for client-side operations.
 * Responsibilities:
 *   1. Load facts from side-effect stores.
 *   2. Call pure business functions.
 *   3. Execute side effects from the returned result.
 *
 * Contains NO business judgment — all decisions live in pure functions.
 */
@Service
public class ClientService {

    private final ClientStore clientStore;
    private final AccountStore accountStore;
    private final AccessStore accessStore;
    private final BankRuleStore bankRuleStore;
    private final TransferLogStore transferLogStore;

    public ClientService(ClientStore clientStore, AccountStore accountStore,
                         AccessStore accessStore, BankRuleStore bankRuleStore,
                         TransferLogStore transferLogStore) {
        this.clientStore = clientStore;
        this.accountStore = accountStore;
        this.accessStore = accessStore;
        this.bankRuleStore = bankRuleStore;
        this.transferLogStore = transferLogStore;
    }

    // ---- createAccount ----

    public AccessFact createAccount(String username, String accountName, AccountType accountType) {
        var client = clientStore.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Client not found: " + username));
        var account = accountStore.save(accountName, accountType);
        return accessStore.save(client.id(), client.username(), true,
            account.accountNo(), account.name(), account.balance(), account.accountType());
    }

    // ---- deposit (no tax on deposits — pure value transfer) ----

    public void deposit(String username, AccountNo destination, Amount amount) {
        var client = clientStore.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Client not found: " + username));
        var destOpt = accountStore.findByNo(destination);
        var currentBalance = destOpt.map(AccountFact::balance).orElse(Amount.ZERO);
        var facts = new DepositDecision.Facts(
            amount, currentBalance, destOpt.isPresent());
        var result = DepositDecision.decide(facts);
        if (!result.allowed()) {
            throw new BusinessException(result.reason());
        }
        accountStore.updateBalance(destOpt.get(), result.newBalance());
    }

    // ---- transfer (with fees) ----

    /**
     * Complete transfer — load facts, call the fat core once, execute effects.
     * Fee calculation is composed internally by {@link TransferDecision}; the
     * orchestration does not know about the fee-computation step.
     */
    public Map<String, Object> transfer(String username, AccountNo source,
                                         AccountNo destination, Amount amount,
                                         boolean isInternal) {
        var client = clientStore.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Client not found: " + username));

        // 1. Prepare all raw facts
        var srcOpt = accountStore.findByNo(source);
        if (srcOpt.isEmpty()) {
            throw new BusinessException("Source account does not exist: " + source);
        }
        var sourceAccount = srcOpt.get();
        var hasAccessRight = accessStore.findByClientAndAccount(client.id(), source).isPresent();
        var destOpt = accountStore.findByNo(destination);
        var destBalance = destOpt.map(AccountFact::balance).orElse(Amount.ZERO);
        int monthlyCount = transferLogStore.countThisMonth(source);
        var rules = bankRuleStore.getAll();
        var feeRules = new TransferDecision.FeeRules(
            requireRule(rules, "transfer.fee.internal"),
            requireRule(rules, "transfer.fee.external.flat"),
            requireRule(rules, "transfer.fee.external.percent"),
            requireRule(rules, "transfer.fee.external.min"),
            requireRule(rules, "transfer.fee.external.max"),
            requireRule(rules, "transfer.tax.threshold"),
            requireRule(rules, "transfer.tax.rate"),
            (int) requireRule(rules, "transfer.savings.free.count"),
            requireRule(rules, "transfer.savings.excess.penalty")
        );

        // 2. One pure-function call — fees composed internally
        var facts = new TransferDecision.Facts(
            amount, isInternal, sourceAccount.accountType(), monthlyCount, feeRules,
            hasAccessRight, sourceAccount.balance(), destOpt.isPresent(), destBalance);
        var result = TransferDecision.decide(facts);
        if (!result.allowed()) {
            throw new BusinessException(result.reason());
        }

        // 3. Execute effects
        accountStore.updateBalance(sourceAccount, result.newSourceBalance());
        accountStore.updateBalance(destOpt.get(), result.newDestinationBalance());
        transferLogStore.record(source, destination, amount, result, isInternal);

        // 4. Response — all fields from the single result
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sourceNewBalance", result.newSourceBalance().toDouble());
        response.put("destNewBalance", result.newDestinationBalance().toDouble());
        response.put("baseFee", result.baseFee().toDouble());
        response.put("excessPenalty", result.excessPenalty().toDouble());
        response.put("transactionTax", result.transactionTax().toDouble());
        response.put("totalFee", result.totalFee().toDouble());
        response.put("isInternal", isInternal);
        return response;
    }

    // ---- addAccountManager ----

    public AccessFact addAccountManager(String username, AccountNo accountNo, String managerUsername) {
        var owner = clientStore.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Client not found: " + username));
        var manager = clientStore.findByUsername(managerUsername)
            .orElseThrow(() -> new NoSuchElementException("Manager not found: " + managerUsername));
        var requesterIsOwner = accessStore.isOwner(owner.id(), accountNo);
        var managerAlreadyHasAccess = accessStore
            .findByClientAndAccount(manager.id(), accountNo).isPresent();
        var facts = new AddAccountManagerDecision.Facts(
            requesterIsOwner, managerAlreadyHasAccess);
        var result = AddAccountManagerDecision.decide(facts);
        if (!result.allowed()) {
            throw new BusinessException(result.reason());
        }
        var account = accountStore.findByNo(accountNo).orElseThrow();
        return accessStore.save(manager.id(), manager.username(), false,
            accountNo, account.name(), account.balance(), account.accountType());
    }

    // ---- findMyAccount ----

    public AccountFact findMyAccount(String username, AccountNo accountNo) {
        var client = clientStore.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Client not found: " + username));
        var account = accountStore.findByNo(accountNo)
            .orElseThrow(() -> new BusinessException(
                "Account %s is not managed by %s.".formatted(accountNo, username)));
        var hasAccess = accessStore.findByClientAndAccount(client.id(), accountNo).isPresent();
        if (!hasAccess) {
            throw new BusinessException(
                "Account %s is not managed by %s.".formatted(accountNo, username));
        }
        return account;
    }

    // ---- accountsReport ----

    public String accountsReport(String username) {
        var client = clientStore.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Client not found: " + username));
        var accesses = accessStore.findByClient(client.id());
        var sb = new StringBuilder();
        sb.append("Accounts of client: ").append(username).append("\n");
        for (var a : accesses) {
            var accessRight = a.isOwner() ? "isOwner" : "manages";
            sb.append("%s\t%s\t%5.2f\t%s\t%s\n".formatted(
                a.accountNo(), accessRight, a.accountBalance().toDouble(),
                a.accountName(), a.accountType()));
        }
        return sb.toString();
    }

    // ---- listMyAccounts (structured, for UI) ----

    public List<Map<String, Object>> myAccounts(String username) {
        var client = clientStore.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Client not found: " + username));
        return accessStore.findByClient(client.id()).stream()
            .map(a -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("accountNo", a.accountNo().number());
                m.put("accountName", a.accountName());
                m.put("balance", a.accountBalance().toDouble());
                m.put("accountType", a.accountType().name());
                m.put("isOwner", a.isOwner());
                return m;
            })
            .toList();
    }

    // ---- myTransfers ----

    public List<Map<String, Object>> myTransfers(String username) {
        var client = clientStore.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Client not found: " + username));
        var accountNos = accessStore.findByClient(client.id()).stream()
            .map(a -> a.accountNo())
            .toList();
        return transferLogStore.findByAccounts(accountNos, 50);
    }

}
