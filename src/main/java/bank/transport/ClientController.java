package bank.transport;

import bank.domain.facts.AccountNo;
import bank.domain.facts.AccountType;
import bank.domain.facts.Amount;
import bank.service.BusinessException;
import bank.service.ClientService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Thin REST adapter for client operations (/client/*).
 * Contains NO business logic — only HTTP parsing, delegation, and response formatting.
 *
 * Authentication: prefers HTTP Basic Auth (Principal), falls back to X-Username header for demo.
 */
@RestController
@RequestMapping("/client")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    /** Extracts the username — Principal first, then X-Username header, then error. */
    private String username(Principal principal, HttpServletRequest request) {
        if (principal != null) {
            return principal.getName();
        }
        var header = request.getHeader("X-Username");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        throw new BusinessException("Authentication required. Set X-Username header.");
    }

    // ---- POST /client/account ----

    @PostMapping("/account")
    public ResponseEntity<Commands.AccessResponse> createAccount(
            @RequestBody Commands.CreateAccountRequest req,
            Principal principal, HttpServletRequest request) {
        AccountType accountType = AccountType.CHECKING;
        if (req.accountType() != null && req.accountType().equalsIgnoreCase("SAVINGS")) {
            accountType = AccountType.SAVINGS;
        }
        var access = clientService.createAccount(username(principal, request),
            req.name(), accountType);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Commands.AccessResponse.from(access));
    }

    // ---- POST /client/deposit ----

    @PostMapping("/deposit")
    public ResponseEntity<Void> deposit(
            @RequestBody Commands.DepositRequest req,
            Principal principal, HttpServletRequest request) {
        clientService.deposit(username(principal, request),
            new AccountNo(req.accountNo()), Amount.ofEuros(req.amount()));
        return ResponseEntity.noContent().build();
    }

    // ---- POST /client/transfer ----

    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transfer(
            @RequestBody Commands.TransferRequest req,
            Principal principal, HttpServletRequest request) {
        var result = clientService.transfer(username(principal, request),
            new AccountNo(req.sourceAccountNo()),
            new AccountNo(req.destinationAccountNo()),
            Amount.ofEuros(req.amount()),
            req.isInternal());
        return ResponseEntity.ok(result);
    }

    // ---- POST /client/manager ----

    @PostMapping("/manager")
    public ResponseEntity<Commands.AccessResponse> addAccountManager(
            @RequestBody Commands.AddAccountManagerRequest req,
            Principal principal, HttpServletRequest request) {
        var access = clientService.addAccountManager(username(principal, request),
            new AccountNo(req.accountNo()), req.username());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Commands.AccessResponse.from(access));
    }

    // ---- GET /client/account ----

    @GetMapping("/account")
    public ResponseEntity<String> accountsReport(
            Principal principal, HttpServletRequest request) {
        var report = clientService.accountsReport(username(principal, request));
        return ResponseEntity.ok(report);
    }

    // ---- GET /client/transfers ----

    @GetMapping("/transfers")
    public ResponseEntity<List<Map<String, Object>>> myTransfers(
            Principal principal, HttpServletRequest request) {
        return ResponseEntity.ok(clientService.myTransfers(username(principal, request)));
    }

    // ---- GET /client/accounts (structured, for UI) ----

    @GetMapping("/accounts")
    public ResponseEntity<List<Map<String, Object>>> myAccounts(
            Principal principal, HttpServletRequest request) {
        return ResponseEntity.ok(clientService.myAccounts(username(principal, request)));
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
