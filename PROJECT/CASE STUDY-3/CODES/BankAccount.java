package casestudy3;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// BankAccount is an ENTITY and the aggregate root for an account's money.
// It is identified by its AccountNumber. Its fields are private, there are no
// setters, and the balance changes only through methods that also write a
// LedgerEntry. So the balance always matches the history.
public final class BankAccount {

    private final AccountNumber number;
    private final String holderName;
    private final OverdraftLimit overdraftLimit;
    private final List<LedgerEntry> ledger = new ArrayList<>();
    private AccountStatus status = AccountStatus.ACTIVE;
    private Money balance = Money.ZERO;
    private long nextSequence = 1;

    public BankAccount(AccountNumber number, String holderName, OverdraftLimit overdraftLimit) {
        this.number = Objects.requireNonNull(number, "account number");
        Objects.requireNonNull(holderName, "holder name");
        if (holderName.isBlank()) {
            throw new BankException("Holder name is required");
        }
        this.holderName = holderName.trim();
        this.overdraftLimit = Objects.requireNonNull(overdraftLimit, "overdraft limit");
    }

    // ---------------- customer operations ----------------

    public void deposit(TransactionAmount amount, String reference) {
        Objects.requireNonNull(amount, "amount");
        requireCanCredit();
        post(EntryType.DEPOSIT, amount, reference);
    }

    public void withdraw(TransactionAmount amount, String reference) {
        Objects.requireNonNull(amount, "amount");
        requireCanDebit(amount);
        post(EntryType.WITHDRAWAL, amount, reference);
    }

    public void freeze() {
        if (status != AccountStatus.ACTIVE) {
            throw new BankException("Only an ACTIVE account can be frozen; status is " + status);
        }
        status = AccountStatus.FROZEN;
    }

    public void unfreeze() {
        if (status != AccountStatus.FROZEN) {
            throw new BankException("Only a FROZEN account can be unfrozen; status is " + status);
        }
        status = AccountStatus.ACTIVE;
    }

    // Closing needs a zero balance, so no money is lost or left unclaimed.
    public void close() {
        if (status == AccountStatus.CLOSED) {
            throw new BankException("Account " + number + " is already closed");
        }
        if (!balance.isZero()) {
            throw new BankException("Account can be closed only with zero balance; balance is " + balance);
        }
        status = AccountStatus.CLOSED;
    }

    // ---------------- rules used by the transfer service ----------------
    // Package-private: only classes in this package (the TransferService) can
    // move money between accounts. Outside code cannot call these.

    void requireCanDebit(TransactionAmount amount) {
        if (status != AccountStatus.ACTIVE) {
            throw new BankException("Account " + number + " is " + status + "; debits are not allowed");
        }
        if (!overdraftLimit.permits(balance, amount)) {
            throw new BankException("Insufficient funds in " + number + ": balance " + balance
                    + ", overdraft limit " + overdraftLimit + ", requested " + amount);
        }
    }

    void requireCanCredit() {
        if (status == AccountStatus.CLOSED) {
            throw new BankException("Account " + number + " is CLOSED; credits are not allowed");
        }
    }

    void postTransferOut(TransactionAmount amount, String reference) {
        requireCanDebit(amount);
        post(EntryType.TRANSFER_OUT, amount, reference);
    }

    void postTransferIn(TransactionAmount amount, String reference) {
        requireCanCredit();
        post(EntryType.TRANSFER_IN, amount, reference);
    }

    // Used only to undo a half-finished transfer. It is recorded as a new credit,
    // so the history shows the debit and its reversal instead of hiding them.
    void postReversal(TransactionAmount amount, String reference) {
        post(EntryType.TRANSFER_IN, amount, reference);
    }

    // ---------------- internal ----------------

    private void post(EntryType type, TransactionAmount amount, String reference) {
        Money signed = type.isCredit() ? amount.money() : Money.ZERO.minus(amount.money());
        balance = balance.plus(signed);
        ledger.add(new LedgerEntry(nextSequence++, type, amount.money(), balance,
                Instant.now(), reference));
    }

    // ---------------- read-only views ----------------

    public AccountNumber number()            { return number; }
    public String holderName()               { return holderName; }
    public AccountStatus status()            { return status; }
    public Money balance()                   { return balance; }
    public OverdraftLimit overdraftLimit()   { return overdraftLimit; }

    // A read-only view. Callers cannot add, remove or change entries.
    public List<LedgerEntry> history() {
        return Collections.unmodifiableList(ledger);
    }

    // Checks the invariant: the balance equals the sum of all ledger movements.
    public boolean isConsistent() {
        Money sum = Money.ZERO;
        for (LedgerEntry e : ledger) {
            sum = sum.plus(e.signedAmount());
        }
        Money last = ledger.isEmpty() ? Money.ZERO : ledger.get(ledger.size() - 1).balanceAfter();
        return sum.equals(balance) && last.equals(balance);
    }

    @Override
    public String toString() {
        return "Account " + number + " (" + holderName + ") " + status + ", balance " + balance;
    }
}
