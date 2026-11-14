package casestudy3;

import java.util.Objects;

// OverdraftLimit is a VALUE OBJECT: how far below zero an account may go.
// It is never negative, so the lowest allowed balance is always -limit.
public final class OverdraftLimit {

    public static final OverdraftLimit NONE = new OverdraftLimit(Money.ZERO);

    private final Money amount;

    private OverdraftLimit(Money amount) {
        this.amount = amount;
    }

    public static OverdraftLimit of(Money amount) {
        Objects.requireNonNull(amount, "overdraft limit");
        if (amount.isNegative()) {
            throw new BankException("Overdraft limit cannot be negative: " + amount);
        }
        return new OverdraftLimit(amount);
    }

    public Money amount() {
        return amount;
    }

    // True if the balance after the debit would still be at or above -limit.
    boolean permits(Money balance, TransactionAmount requested) {
        Money lowestAllowed = Money.ZERO.minus(amount);
        return balance.minus(requested.money()).compareTo(lowestAllowed) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof OverdraftLimit l && amount.equals(l.amount);
    }

    @Override
    public int hashCode() {
        return amount.hashCode();
    }

    @Override
    public String toString() {
        return amount.toString();
    }
}
