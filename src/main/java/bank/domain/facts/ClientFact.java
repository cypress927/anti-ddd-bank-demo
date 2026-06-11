package bank.domain.facts;

import java.time.LocalDate;

/**
 * Pure data record: fixed-size projection of a client.
 * No behavior, no injected dependencies — just the facts.
 */
public record ClientFact(Long id, String username, LocalDate birthDate) {

    public ClientFact {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (birthDate == null) {
            throw new IllegalArgumentException("birthDate is required");
        }
    }

    /** For a client not yet persisted (id == null). */
    public static ClientFact unsaved(String username, LocalDate birthDate) {
        return new ClientFact(null, username, birthDate);
    }

    /** Return a copy with the given id (post-save). */
    public ClientFact withId(Long id) {
        return new ClientFact(id, this.username, this.birthDate);
    }
}
