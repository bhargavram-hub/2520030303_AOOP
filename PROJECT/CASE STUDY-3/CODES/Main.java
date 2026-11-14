package casestudy3;

// Demo program for Case Study 3 (Bank Account and Money Transfers).
// It shows normal operations, then shows that each rule from the case study is enforced.
public class Main {

    public static void main(String[] args) {

        BankAccount ravi = new BankAccount(AccountNumber.of("1000000001"), "Ravi Kumar",
                OverdraftLimit.of(Money.ofRupees("500")));
        BankAccount priya = new BankAccount(AccountNumber.of("1000000002"), "Priya Sharma",
                OverdraftLimit.NONE);
        TransferService transfers = new TransferService();

        System.out.println("---- Normal operations ----");
        ravi.deposit(TransactionAmount.ofRupees("20000"), "Salary");
        ravi.withdraw(TransactionAmount.ofRupees("9800"), "ATM");
        ravi.withdraw(TransactionAmount.ofRupees("10700"), "Rent");   // ends at -500, exactly the limit
        System.out.println(ravi);

        System.out.println();
        System.out.println("---- Rules are enforced ----");
        tryIt("withdraw beyond overdraft limit (Rs 1 more)",
                () -> ravi.withdraw(TransactionAmount.ofRupees("1"), "ATM"));
        tryIt("deposit of zero",
                () -> ravi.deposit(TransactionAmount.ofRupees("0"), "Bad"));
        tryIt("deposit of negative amount",
                () -> ravi.deposit(TransactionAmount.ofRupees("-100"), "Bad"));
        tryIt("amount with 3 decimal places",
                () -> TransactionAmount.ofRupees("10.555"));
        tryIt("overdraft limit negative",
                () -> OverdraftLimit.of(Money.ofRupees("-1")));
        tryIt("account number with letters",
                () -> AccountNumber.of("10000ABC01"));
        tryIt("empty holder name",
                () -> new BankAccount(AccountNumber.of("1000000003"), " ", OverdraftLimit.NONE));

        System.out.println();
        System.out.println("---- Freeze ----");
        ravi.freeze();
        tryIt("withdraw from FROZEN account",
                () -> ravi.withdraw(TransactionAmount.ofRupees("100"), "ATM"));
        tryIt("freeze an account that is already frozen",
                ravi::freeze);
        ravi.deposit(TransactionAmount.ofRupees("1000"), "Cheque");   // deposits still allowed
        System.out.println("After deposit into frozen account: " + ravi);
        ravi.unfreeze();
        System.out.println("After unfreeze: " + ravi);

        System.out.println();
        System.out.println("---- Transfers ----");
        tryIt("transfer Rs 2000 with only Rs 500 balance and Rs 500 overdraft",
                () -> transfers.transfer(ravi, priya, TransactionAmount.ofRupees("2000")));
        ravi.deposit(TransactionAmount.ofRupees("2000"), "Bonus");
        System.out.println("Transfer Rs 2000 from Ravi to Priya -> "
                + transfers.transfer(ravi, priya, TransactionAmount.ofRupees("2000")));
        System.out.println(ravi);
        System.out.println(priya);

        System.out.println();
        System.out.println("---- Failure during transfer (simulated crash after debit) ----");
        Money ravisBefore = ravi.balance();
        Money priyaBefore = priya.balance();
        transfers.injectFault(() -> {
            throw new IllegalStateException("system crash before credit");
        });
        tryIt("transfer Rs 1000 during crash",
                () -> transfers.transfer(ravi, priya, TransactionAmount.ofRupees("1000")));
        transfers.injectFault(() -> { });
        System.out.println("Ravi before: " + ravisBefore + ", after: " + ravi.balance()
                + (ravisBefore.equals(ravi.balance()) ? "  (no money lost)" : "  (PROBLEM)"));
        System.out.println("Priya before: " + priyaBefore + ", after: " + priya.balance()
                + (priyaBefore.equals(priya.balance()) ? "  (unchanged)" : "  (PROBLEM)"));

        System.out.println();
        System.out.println("---- Closing ----");
        tryIt("close Priya while she has a balance",
                priya::close);
        priya.withdraw(TransactionAmount.ofRupees("2000"), "Cash out");
        priya.close();
        System.out.println("Closed: " + priya);
        tryIt("transfer into a CLOSED account",
                () -> transfers.transfer(ravi, priya, TransactionAmount.ofRupees("100")));

        System.out.println();
        System.out.println("---- Ledgers ----");
        System.out.println("History of " + ravi.number() + ":");
        for (LedgerEntry e : ravi.history()) {
            System.out.println("  " + e);
        }
        System.out.println("History of " + priya.number() + ":");
        for (LedgerEntry e : priya.history()) {
            System.out.println("  " + e);
        }
        System.out.println("Ravi balance matches ledger: " + ravi.isConsistent());
        System.out.println("Priya balance matches ledger: " + priya.isConsistent());
    }

    private static void tryIt(String what, Runnable action) {
        try {
            action.run();
            System.out.println(what + " -> NOT rejected (problem!)");
        } catch (RuntimeException e) {
            System.out.println(what + " -> rejected: " + e.getMessage());
        }
    }
}
