package casestudy3;

import java.util.Objects;

// Transfer is an ENTITY: it has its own identity (transfer id) and a lifecycle
// (PENDING -> COMPLETED or FAILED). Once it is COMPLETED or FAILED it cannot change.
public final class Transfer {

    private final String id;
    private final AccountNumber from;
    private final AccountNumber to;
    private final TransactionAmount amount;
    private TransferStatus status = TransferStatus.PENDING;

    Transfer(String id, AccountNumber from, AccountNumber to, TransactionAmount amount) {
        this.id = Objects.requireNonNull(id);
        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
        this.amount = Objects.requireNonNull(amount);
        if (from.equals(to)) {
            throw new BankException("Cannot transfer money to the same account");
        }
    }

    void markCompleted() {
        requirePending();
        status = TransferStatus.COMPLETED;
    }

    void markFailed() {
        requirePending();
        status = TransferStatus.FAILED;
    }

    private void requirePending() {
        if (status != TransferStatus.PENDING) {
            throw new BankException("Transfer " + id + " is already " + status);
        }
    }

    public String id()                 { return id; }
    public AccountNumber from()        { return from; }
    public AccountNumber to()          { return to; }
    public TransactionAmount amount()  { return amount; }
    public TransferStatus status()     { return status; }

    @Override
    public String toString() {
        return id + ": " + from + " -> " + to + " " + amount + " [" + status + "]";
    }
}
