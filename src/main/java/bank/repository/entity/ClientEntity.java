package bank.repository.entity;

import bank.domain.facts.ClientFact;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * JPA entity for the "clients" table — PURE INFRASTRUCTURE.
 * Lives only in repository layer. Never leaks into domain.
 */
@Entity
@Table(name = "clients")
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private LocalDate birthDate;

    protected ClientEntity() {}

    public ClientEntity(String username, LocalDate birthDate) {
        this.username = username;
        this.birthDate = birthDate;
    }

    public ClientFact toFact() {
        return new ClientFact(this.id, this.username, this.birthDate);
    }

    public Long getId()                    { return id; }
    public void setId(Long id)            { this.id = id; }

    public String getUsername()            { return username; }
    public void setUsername(String u)      { this.username = u; }

    public LocalDate getBirthDate()        { return birthDate; }
    public void setBirthDate(LocalDate d)  { this.birthDate = d; }

    @Override
    public String toString() {
        return "ClientEntity{id=%d, username='%s', birthDate='%s'}"
            .formatted(id, username, birthDate);
    }
}
