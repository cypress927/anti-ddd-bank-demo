package bank.service;

import bank.domain.decisions.AddAccountManagerDecision;
import bank.domain.decisions.DepositDecision;
import bank.domain.decisions.TransferDecision;
import bank.domain.facts.AccountFact;
import bank.domain.facts.AccountNo;
import bank.domain.facts.AccessFact;
import bank.domain.facts.Amount;
import bank.domain.facts.ClientFact;
import bank.repository.AccessStore;
import bank.repository.AccountStore;
import bank.repository.ClientStore;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public ClientService(ClientStore clientStore, AccountStore accountStore,
                         AccessStore accessStore) {
        this.clientStore = clientStore;
        this.accountStore = accountStore;
        this.accessStore = accessStore;
    }

    // ---- createAccount (mechanical — no pure business rule to apply) ----

    public AccessFact createAccount(String username, String accountName) {
        var client = clientStore.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Client not found: " + username));
        // Mechanical: create account, create owner access
        var account = accountStore.save(accountName);
        return accessStore.save(client.id(), client.username(), true,
            account.accountNo(), account.name(), account.balance());
    }

    // ---- deposit ----

    public void deposit(String username, AccountNo destination, Amount amount) {
        var client = clientStore.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Client not found: " + username));
        // 1. Prepare facts
        var destOpt = accountStore.findByNo(destination);
        var currentBalance = destOpt.map(AccountFact::balance).orElse(Amount.ZERO);
        var facts = new DepositDecision.Facts(
            amount, currentBalance, destOpt.isPresent());
        // 2. Pure business computation
        var result = DepositDecision.decide(facts);
        // 3. Execute effects — use already-loaded fact to avoid extra SQL SELECT
        if (!result.allowed()) {
            throw new BusinessException(result.reason());
        }
        accountStore.updateBalance(destOpt.get(), result.newBalance());
    }

    // ---- transfer ----

    public void transfer(String username, AccountNo source, AccountNo destination, Amount amount) {
        var client = clientStore.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Client not found: " + username));
        // 1. Prepare facts — load everything the pure function needs
        var srcOpt = accountStore.findByNo(source);
        if (srcOpt.isEmpty()) {
            throw new BusinessException("Source account does not exist: " + source);
        }
        var sourceAccount = srcOpt.get();
        var hasAccessRight = accessStore.findByClientAndAccount(client.id(), source).isPresent();
        var destOpt = accountStore.findByNo(destination);
        var destBalance = destOpt.map(AccountFact::balance).orElse(Amount.ZERO);
        var facts = new TransferDecision.Facts(
            amount, hasAccessRight, sourceAccount.balance(),
            destOpt.isPresent(), destBalance);
        // 2. Pure business computation — computes BOTH new balances
        var result = TransferDecision.decide(facts);
        // 3. Execute effects — both balances come from the pure function
        if (!result.allowed()) {
            throw new BusinessException(result.reason());
        }
        accountStore.updateBalance(sourceAccount, result.newSourceBalance());
        accountStore.updateBalance(destOpt.get(), result.newDestinationBalance());
    }

    // ---- addAccountManager ----

    public AccessFact addAccountManager(String username, AccountNo accountNo, String managerUsername) {
        var owner = clientStore.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Client not found: " + username));
        var manager = clientStore.findByUsername(managerUsername)
            .orElseThrow(() -> new NoSuchElementException("Manager not found: " + managerUsername));
        // 1. Prepare facts
        var requesterIsOwner = accessStore.isOwner(owner.id(), accountNo);
        var managerAlreadyHasAccess = accessStore
            .findByClientAndAccount(manager.id(), accountNo).isPresent();
        var facts = new AddAccountManagerDecision.Facts(
            requesterIsOwner, managerAlreadyHasAccess);
        // 2. Pure business computation
        var result = AddAccountManagerDecision.decide(facts);
        // 3. Execute effects
        if (!result.allowed()) {
            throw new BusinessException(result.reason());
        }
        var account = accountStore.findByNo(accountNo).orElseThrow();
        return accessStore.save(manager.id(), manager.username(), false,
            accountNo, account.name(), account.balance());
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
            sb.append("%s\t%s\t%5.2f\t%s\n".formatted(
                a.accountNo(), accessRight, a.accountBalance().toDouble(), a.accountName()));
        }
        return sb.toString();
    }
}
