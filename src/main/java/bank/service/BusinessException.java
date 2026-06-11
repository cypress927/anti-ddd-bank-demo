package bank.service;

/**
 * Signals a business rule violation. Carries the reason from the pure decision.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String reason) {
        super(reason);
    }
}
