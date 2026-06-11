package bank.service;

import bank.domain.decisions.CreateClientDecision;
import bank.domain.decisions.DeleteClientDecision;
import bank.domain.facts.Amount;
import bank.domain.facts.ClientFact;
import bank.repository.AccessStore;
import bank.repository.AccountStore;
import bank.repository.ClientStore;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

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

    public BankService(ClientStore clientStore, AccountStore accountStore,
                       AccessStore accessStore) {
        this.clientStore = clientStore;
        this.accountStore = accountStore;
        this.accessStore = accessStore;
    }

    // ---- createClient ----

    public ClientFact createClient(String username, LocalDate birthDate) {
        // 1. Prepare facts
        var facts = new CreateClientDecision.Facts(
            username, clientStore.existsByUsername(username));
        // 2. Pure business computation
        var result = CreateClientDecision.decide(facts);
        // 3. Execute effects
        if (!result.allowed()) {
            throw new BusinessException(result.reason());
        }
        return clientStore.save(username, birthDate);
    }

    // ---- deleteClient ----

    public void deleteClient(String username) {
        var client = clientStore.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("Client not found: " + username));
        // 1. Prepare facts
        var facts = new DeleteClientDecision.Facts(
            accessStore.ownsAnyAccount(client.id()));
        // 2. Pure business computation
        var result = DeleteClientDecision.decide(facts);
        // 3. Execute effects
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
        // Mechanical preparation: find which clients have rich accounts.
        // The "rich" judgment (>= minBalance) is done by the store query,
        // which projects source data into a boolean existence fact.
        var richAccesses = accessStore.findFullAccounts(minBalance);
        return richAccesses.stream()
            .map(a -> clientStore.findById(a.clientId()))
            .flatMap(java.util.Optional::stream)
            .distinct()
            .toList();
    }
}
