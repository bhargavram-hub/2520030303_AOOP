package casestudy3;

// The kinds of movement that can appear in an account's ledger.
public enum EntryType {
    DEPOSIT(true),
    TRANSFER_IN(true),
    WITHDRAWAL(false),
    TRANSFER_OUT(false);

    private final boolean credit;

    EntryType(boolean credit) {
        this.credit = credit;
    }

    // A credit increases the balance, a debit decreases it.
    public boolean isCredit() {
        return credit;
    }
}
