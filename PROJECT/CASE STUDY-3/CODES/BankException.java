package casestudy3;

// Thrown whenever a business rule of the bank is broken.
public class BankException extends RuntimeException {

    public BankException(String message) {
        super(message);
    }
}
