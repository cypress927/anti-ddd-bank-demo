package bank.transport;

import bank.domain.facts.Amount;
import bank.service.BankService;
import bank.service.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Thin REST adapter for bank-clerk operations (/bank/*).
 * Contains NO business logic — only HTTP parsing, delegation, and response formatting.
 *
 * GET  endpoints are public (clients can view rates, transfers).
 * POST/PUT/DELETE require banker authentication via X-Banker-Key header.
 */
@RestController
@RequestMapping("/bank")
public class BankController {

    /** Demo banker access key — validated on mutation endpoints. */
    private static final String BANKER_KEY = "banker123";

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    /** Require banker key for mutation endpoints. */
    private void requireBanker(HttpServletRequest request) {
        var key = request.getHeader("X-Banker-Key");
        if (key == null || !key.equals(BANKER_KEY)) {
            throw new BusinessException("Banker authentication required. Set X-Banker-Key header.");
        }
    }

    // ---- POST /bank/login ----

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Commands.BankerLoginRequest req) {
        if (!"banker".equals(req.username()) || !BANKER_KEY.equals(req.password())) {
            throw new BusinessException("Invalid banker credentials.");
        }
        return ResponseEntity.ok(Map.of("token", BANKER_KEY, "role", "banker"));
    }

    // ---- GET / (public) ----

    @GetMapping("/")
    public ResponseEntity<String> home() {
        return ResponseEntity.ok("""
            <!DOCTYPE html><html><body>
            <h1>Anti-DDD Bank — Pure Function Architecture</h1>
            <p>Bank REST API is running.</p>
            </body></html>""");
    }

    // ---- POST /bank/client ----

    @PostMapping("/client")
    public ResponseEntity<Commands.ClientResponse> createClient(
            @RequestBody Commands.CreateClientRequest req,
            HttpServletRequest request) {
        requireBanker(request);
        if (req.id() != null) {
            throw new BusinessException(
                "New client must not have an id, but got: " + req.id());
        }
        var birthDate = LocalDate.parse(req.birthDate());
        var fact = bankService.createClient(req.username(), birthDate);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Commands.ClientResponse.from(fact));
    }

    // ---- DELETE /bank/client/{username} ----

    @DeleteMapping("/client/{username}")
    public ResponseEntity<Void> deleteClient(@PathVariable String username,
                                              HttpServletRequest request) {
        requireBanker(request);
        bankService.deleteClient(username);
        return ResponseEntity.noContent().build();
    }

    // ---- GET /bank/client (public — clients need to see who exists) ----

    @GetMapping("/client")
    public ResponseEntity<List<Commands.ClientResponse>> findClients(
            @RequestParam(defaultValue = "") String fromBirth,
            @RequestParam(defaultValue = "") String minBalance) {
        List<bank.domain.facts.ClientFact> clients;
        if (!fromBirth.isEmpty() && !minBalance.isEmpty()) {
            throw new BusinessException(
                "Must not provide both parameters: fromBirth and minBalance.");
        } else if (!fromBirth.isEmpty()) {
            clients = bankService.findYoungClients(LocalDate.parse(fromBirth));
        } else if (!minBalance.isEmpty()) {
            clients = bankService.findRichClients(Amount.ofEuros(Double.parseDouble(minBalance)));
        } else {
            clients = bankService.findAllClients();
        }
        var response = clients.stream().map(Commands.ClientResponse::from).toList();
        return ResponseEntity.ok(response);
    }

    // ---- POST /bank/pair (demo) ----

    @PostMapping("/pair")
    public ResponseEntity<List<Commands.ClientResponse>> create2Clients(
            @RequestParam(defaultValue = "1") long seed,
            HttpServletRequest request) {
        requireBanker(request);
        var client1 = bankService.createClient("hans" + seed,
            LocalDate.now().minusYears(20 + seed % 50));
        System.out.printf("Client %s created.%n", client1);
        if (seed % 3 == 0) {
            throw new BusinessException(
                "Simulated failure after creating " + client1.username());
        }
        var client2 = bankService.createClient("jana" + seed,
            LocalDate.now().minusYears(18 + seed % 40));
        System.out.printf("Client %s created.%n", client2);
        var all = bankService.findAllClients();
        return ResponseEntity.ok(
            all.stream().map(Commands.ClientResponse::from).toList());
    }

    // ---- GET /bank/rules (public — clients can view rates) ----

    @GetMapping("/rules")
    public ResponseEntity<List<Map<String, Object>>> getRules() {
        return ResponseEntity.ok(bankService.getAllRules());
    }

    // ---- PUT /bank/rules ----

    @PutMapping("/rules")
    public ResponseEntity<Map<String, Object>> updateRule(
            @RequestBody Commands.UpdateRuleRequest req,
            HttpServletRequest request) {
        requireBanker(request);
        bankService.updateRule(req.ruleKey(), req.value());
        return ResponseEntity.ok(Map.of("ruleKey", req.ruleKey(), "value", req.value()));
    }

    // ---- POST /bank/accrue-interest ----

    @PostMapping("/accrue-interest")
    public ResponseEntity<List<Map<String, Object>>> accrueInterest(
            @RequestBody Commands.AccrueInterestRequest req,
            HttpServletRequest request) {
        requireBanker(request);
        var results = bankService.accrueInterest(req.days());
        return ResponseEntity.ok(results);
    }

    // ---- GET /bank/transfers (public — clients may want to see network stats) ----

    @GetMapping("/transfers")
    public ResponseEntity<List<Map<String, Object>>> recentTransfers(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(bankService.recentTransfers(limit));
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
