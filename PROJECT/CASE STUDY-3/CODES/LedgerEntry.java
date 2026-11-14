package casestudy3;

import java.time.Instant;
import java.util.Objects;

// LedgerEntry is a VALUE OBJECT that records one movement on an account.
// It is immutable: there are no setters, and only BankAccount can create one.
// Once written, an entry is never changed or deleted. Corrections are new entries.
public final class LedgerEntry {

    private final long sequence;      // position in the account's history
    private final EntryType type;
    private final Money amount;        // always positive: the size of the movement
    private final Money balanceAfter; // balance once this entry is applied
    private final Instant at;
    private final String reference;

    LedgerEntry(long sequence, EntryType type, Money amount, Money balanceAfter,
                Instant at, String reference) {
        this.sequence = sequence;
        this.type = Objects.requireNonNull(type);
        this.amount = Objects.requireNonNull(amount);
        this.balanceAfter = Objects.requireNonNull(balanceAfter);
        this.at = Objects.requireNonNull(at);
        this.reference = Objects.requireNonNull(reference);
    }

    public long sequence()        { return sequence; }
    public EntryType type()       { return type; }
    public Money amount()         { return amount; }
    public Money balanceAfter()   { return balanceAfter; }
    public Instant at()          { return at; }
    public String reference()     { return reference; }

    // The effect this entry has on the balance: +amount for credits, -amount for debits.
    public Money signedAmount() {
        return type.isCredit() ? amount : Money.ZERO.minus(amount);
    }

    @Override
    public String toString() {
        return "#" + sequence + " " + type + " " + amount
                + " -> balance " + balanceAfter + " (" + reference + ")";
    }
}
