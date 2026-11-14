package casestudy3;

import java.math.BigDecimal;
import java.util.Objects;

// Money is a VALUE OBJECT: an amount of rupees stored as a long number of paise
// (1 rupee = 100 paise). A long is exact, so there are no floating point errors.
// Money may be negative, because a balance goes below zero when an account is
// overdrawn. A TRANSACTION amount is a different type (TransactionAmount) that
// can never be zero or negative.
public final class Money implements Comparable<Money> {

    public static final Money ZERO = new Money(0);

    private final long paise;

    private Money(long paise) {
        this.paise = paise;
    }

    public static Money ofPaise(long paise) {
        return new Money(paise);
    }

    // Accepts "1500", "250.5", "-20.25". Rejects more than 2 decimal places.
    public static Money ofRupees(String rupees) {
        Objects.requireNonNull(rupees, "amount");
        BigDecimal inPaise;
        try {
            inPaise = new BigDecimal(rupees.trim()).movePointRight(2);
        } catch (NumberFormatException e) {
            throw new BankException("Not a valid amount: " + rupees);
        }
        if (inPaise.stripTrailingZeros().scale() > 0) {
            throw new BankException("At most 2 decimal places allowed: " + rupees);
        }
        return new Money(inPaise.longValueExact());
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(paise, other.paise));
    }

    public Money minus(Money other) {
        return new Money(Math.subtractExact(paise, other.paise));
    }

    public boolean isPositive() { return paise > 0; }
    public boolean isNegative() { return paise < 0; }
    public boolean isZero()     { return paise == 0; }

    public long paise() {
        return paise;
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(paise, other.paise);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money m && paise == m.paise;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(paise);
    }

    @Override
    public String toString() {
        long abs = Math.abs(paise);
        return "INR " + (paise < 0 ? "-" : "") + (abs / 100) + "." + String.format("%02d", abs % 100);
    }
}
