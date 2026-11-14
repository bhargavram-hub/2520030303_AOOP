package casestudy3;

import java.util.Objects;

// TransferService moves money between two accounts as ONE operation: either both
// sides are applied, or neither is. Real banks use a database transaction for this.
// Here the same idea is shown in memory: check first, debit, credit, and if the
// credit fails, reverse the debit (a compensating entry).
public final class TransferService {

    private int nextId = 1;
    private Runnable faultInjector = () -> { };   // demo only: simulates a failure

    // Package-private: used by Main to simulate a crash between the two steps.
    void injectFault(Runnable fault) {
        this.faultInjector = Objects.requireNonNull(fault);
    }

    // synchronized: two transfers cannot interleave and see each other's half-done state.
    public synchronized Transfer transfer(BankAccount from, BankAccount to, TransactionAmount amount) {
        Objects.requireNonNull(from, "source account");
        Objects.requireNonNull(to, "destination account");
        Objects.requireNonNull(amount, "amount");
        if (from == to) {
            throw new BankException("Cannot transfer money to the same account");
        }

        // Step 1: check every rule BEFORE changing anything.
        // If a check fails here, no money has moved.
        from.requireCanDebit(amount);
        to.requireCanCredit();

        Transfer transfer = new Transfer("TRF-" + nextId++, from.number(), to.number(), amount);

        // Step 2: debit the source.
        from.postTransferOut(amount, transfer.id());

        // Step 3: credit the destination. If anything goes wrong here, undo step 2.
        try {
            faultInjector.run();
            to.postTransferIn(amount, transfer.id());
        } catch (RuntimeException e) {
            from.postReversal(amount, transfer.id() + " REVERSAL");
            transfer.markFailed();
            throw new BankException("Transfer " + transfer.id()
                    + " failed and was reversed: " + e.getMessage());
        }

        transfer.markCompleted();
        return transfer;
    }
}
