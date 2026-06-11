package bank.transport;

import bank.domain.facts.AccountNo;
import bank.domain.facts.Amount;
import bank.service.BusinessException;
import bank.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.NoSuchElementException;

/**
 * Thin REST adapter for client operations (/client/*).
 * Contains NO business logic — only HTTP parsing, delegation, and response formatting.
 *
 * Authentication: uses HTTP Basic Auth — the Principal name is the client's username.
 */
@RestController
@RequestMapping("/client")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    /** Extracts the authenticated username from the request. */
    private String username(Principal principal) {
        if (principal == null) {
            throw new BusinessException("Authentication required.");
        }
        return principal.getName();
    }

    // ---- POST /client/account ----

    @PostMapping("/account")
    public ResponseEntity<Commands.AccessResponse> createAccount(
            @RequestBody Commands.CreateAccountRequest req,
            Principal principal) {
        var access = clientService.createAccount(username(principal), req.name());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Commands.AccessResponse.from(access));
    }

    // ---- POST /client/deposit ----

    @PostMapping("/deposit")
    public ResponseEntity<Void> deposit(
            @RequestBody Commands.DepositRequest req,
            Principal principal) {
        clientService.deposit(username(principal),
            new AccountNo(req.accountNo()), Amount.ofEuros(req.amount()));
        return ResponseEntity.noContent().build();
    }

    // ---- POST /client/transfer ----

    @PostMapping("/transfer")
    public ResponseEntity<Commands.AccountResponse> transfer(
            @RequestBody Commands.TransferRequest req,
            Principal principal) {
        clientService.transfer(username(principal),
            new AccountNo(req.sourceAccountNo()),
            new AccountNo(req.destinationAccountNo()),
            Amount.ofEuros(req.amount()));
        var srcAccount = clientService.findMyAccount(username(principal),
            new AccountNo(req.sourceAccountNo()));
        return ResponseEntity.noContent().build();
    }

    // ---- POST /client/manager ----

    @PostMapping("/manager")
    public ResponseEntity<Commands.AccessResponse> addAccountManager(
            @RequestBody Commands.AddAccountManagerRequest req,
            Principal principal) {
        var access = clientService.addAccountManager(username(principal),
            new AccountNo(req.accountNo()), req.username());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Commands.AccessResponse.from(access));
    }

    // ---- GET /client/account ----

    @GetMapping("/account")
    public ResponseEntity<String> accountsReport(Principal principal) {
        var report = clientService.accountsReport(username(principal));
        return ResponseEntity.ok(report);
    }

    // ---- exception handlers ----

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusiness(BusinessException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
