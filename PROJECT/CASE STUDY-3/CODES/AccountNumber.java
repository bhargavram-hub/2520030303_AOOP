package casestudy3;

import java.util.Objects;

// AccountNumber is a VALUE OBJECT. Two account numbers with the same digits are the
// same number. The format is checked when it is created, so an invalid number
// (letters, wrong length) can never be stored in an account.
public final class AccountNumber {

    private final String value;

    private AccountNumber(String value) {
        this.value = value;
    }

    public static AccountNumber of(String raw) {
        Objects.requireNonNull(raw, "account number");
        String digits = raw.replace(" ", "").trim();
        if (!digits.matches("\\d{10}")) {
            throw new BankException("Account number must be exactly 10 digits: " + raw);
        }
        return new AccountNumber(digits);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AccountNumber a && value.equals(a.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
