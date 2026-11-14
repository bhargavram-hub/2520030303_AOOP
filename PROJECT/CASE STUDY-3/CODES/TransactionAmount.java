package casestudy3;

import java.util.Objects;

// TransactionAmount is a VALUE OBJECT that can only hold a strictly positive amount.
// Every deposit, withdrawal and transfer takes a TransactionAmount, so a zero or
// negative transaction cannot even be passed to an account method.
public final class TransactionAmount {

    private final Money money;

    private TransactionAmount(Money money) {
        this.money = money;
    }

    public static TransactionAmount of(Money money) {
        Objects.requireNonNull(money, "amount");
        if (!money.isPositive()) {
            throw new BankException("Transaction amount must be greater than zero: " + money);
        }
        return new TransactionAmount(money);
    }

    public static TransactionAmount ofRupees(String rupees) {
        return of(Money.ofRupees(rupees));
    }

    public Money money() {
        return money;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TransactionAmount t && money.equals(t.money);
    }

    @Override
    public int hashCode() {
        return money.hashCode();
    }

    @Override
    public String toString() {
        return money.toString();
    }
}
