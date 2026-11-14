# CO-1 Case Study 3 — Completed Answers

## Problems Identified

**Problems we identified in the proposed (naive) basic implementation:**

- Balances and amounts are `double` or `int` fields, so amounts can have rounding errors and can be stored with more decimal places than rupees allow.
- Fields are public, so any class can set `balance` directly, bypassing every rule. This is how withdrawals beyond the overdraft limit happen.
- Account status is a plain string or boolean, and the withdraw service does not check it. This is why frozen accounts still allow withdrawals.
- Deposit, withdraw and transfer are separate service methods. Nothing ties them together, so a transfer can debit one account and then fail before crediting the other.
- The transaction history is a separate list that is updated by hand. Nothing forces the balance and the history to change together, so they drift apart.
- Zero and negative amounts are accepted because the amount is a plain number. Nothing checks it at the point where it enters the system.

The fix in our design: money values and amounts are small value objects that check themselves, the account is the only object that can change its balance, every change writes a ledger entry in the same step, and a transfer is one operation that either finishes on both sides or not at all.

## Question 1

**Answer:**

We identified seven invariants that the banking application must always keep true.

**1. The balance never goes below the overdraft limit.** The balance of an account can be negative, but never lower than minus the overdraft limit (for example, minus Rs 500 for a Rs 500 limit).
- *Why it matters:* The overdraft limit is the bank's risk limit for that customer. It is the rule that the customer is lending against.
- *Consequences if violated:* The bank lends more than it approved. The customer may owe an amount the bank cannot recover, and the bank may breach its own regulatory exposure limits. This is the problem "customers are withdrawing beyond their overdraft limits".

**2. Every transaction amount is greater than zero and has at most two decimal places.**
- *Why it matters:* A deposit or withdrawal of zero or a negative amount has no business meaning. Amounts with a fraction of a paisa cannot be settled.
- *Consequences if violated:* A negative deposit works as a hidden withdrawal and bypasses the withdrawal checks. A zero transaction creates empty ledger lines. Fractional paise cause the balance and the statement to disagree.

**3. A frozen account cannot be debited.** Withdrawals and transfers out are rejected while the status is FROZEN.
- *Why it matters:* Freezing is the bank's protection for suspected fraud, a legal order or a dispute. The purpose is to stop money leaving the account.
- *Consequences if violated:* Money can leave an account that the bank has frozen. This may break a court order or allow fraud to continue after it has been detected.

**4. A closed account has zero balance, and it cannot be debited or credited afterwards.**
- *Why it matters:* A closed account must not hold money that nobody can claim, and it must not change after it has closed.
- *Consequences if violated:* Money is left stranded in a closed account, or money is moved into an account that the customer no longer uses, where it may be lost.

**5. The balance always equals the sum of the ledger entries.**
- *Why it matters:* The balance is only a summary. The ledger is the proof of how it was reached. If the two disagree, the bank cannot explain the balance to the customer or an auditor.
- *Consequences if violated:* The statement and the balance differ. This is the problem "account balances do not always match the transaction history".

**6. A transfer is atomic: either both accounts change, or neither does.**
- *Why it matters:* A transfer moves money. Half a transfer creates or destroys money.
- *Consequences if violated:* Money is deducted from the sender and never reaches the receiver. The customer loses money and the bank's books no longer balance. This is the problem "money is deducted but not credited during a system failure".

**7. Ledger entries are never changed or deleted.**
- *Why it matters:* The history is the audit record. Banking regulators require that past records cannot be altered without a trace.
- *Consequences if violated:* Past transactions can be hidden or edited, which makes fraud and mistakes impossible to detect.

## Question 2

**Answer:**

| Concept | Entity or Value Object | Justification |
|---|---|---|
| Bank Account | Entity | Identified by its account number. The balance, status and ledger change over time, but it is still the same account. Two accounts with the same balance are different accounts. |
| Account Number | Value Object | Identified only by its digits. Two identical account numbers are the same number. It has no lifecycle and is never changed. |
| Money | Value Object | Known only by its amount (for example Rs 500.00). Two Rs 500.00 amounts are interchangeable. It is immutable: adding money creates a new Money object. |
| Ledger Entry | Value Object (immutable record) | Once posted, an entry never changes or leaves the ledger. Two entries with the same type, amount, time and reference describe the same movement. It is identified only by its position in the account's history, so it has no lifecycle of its own. |
| Overdraft Limit | Value Object | Defined only by its amount. Two limits of Rs 500 are the same. When the limit changes, the bank replaces it with a new value instead of editing the old one. |
| Fund Transfer | Entity | Has its own identity (a transfer id), which is what the bank quotes to the customer and links to the ledger. It has a lifecycle: PENDING, then COMPLETED or FAILED. Two transfers of the same amount between the same accounts are still different transfers. |

**Justification based on identity, equality, lifecycle and mutability:**

- **Entities** (Bank Account, Fund Transfer) have a unique identity. Equality means "same identity", not "same values". Both change state over time, and their state changes follow business rules.
- **Value objects** (Account Number, Money, Ledger Entry, Overdraft Limit) are equal when their values are equal. They are immutable, so they can be shared safely between objects and can never be changed behind the back of the account that uses them.

Ledger Entry is the least obvious case. It has a sequence number, but that number is only its position. It is never updated, so it behaves as a value. Treating it as an entity would suggest that entries can be edited, which is exactly what the bank forbids.

## Question 3

**Answer:**

**Design.** The main class is `BankAccount`, the aggregate root for money. Every money value is a value object, and every change to money is a method on the account.

```
BankAccount (Entity, aggregate root)
  - number: AccountNumber          (value)
  - overdraftLimit: OverdraftLimit (value)
  - status: AccountStatus          (enum: ACTIVE, FROZEN, CLOSED)
  - balance: Money                 (value, may be negative)
  - ledger: List<LedgerEntry>      (private, exposed only as an unmodifiable view)
  + deposit(TransactionAmount, reference)
  + withdraw(TransactionAmount, reference)
  + freeze() / unfreeze() / close()
```

The operations, and how each one is protected:

- **Deposit:** `deposit(TransactionAmount amount, String reference)`. The amount type `TransactionAmount` can only hold a value greater than zero, so a zero or negative deposit cannot be created at all. `requireCanCredit()` rejects a closed account. The method then adds the amount to the balance and writes a `DEPOSIT` ledger entry, both in the same method.
- **Withdraw:** `withdraw(TransactionAmount amount, String reference)`. `requireCanDebit()` checks in order: the account must be ACTIVE (this is how frozen and closed accounts are blocked), and the balance minus the amount must not go below minus the overdraft limit. Only if both checks pass is the balance changed and a `WITHDRAWAL` entry written.
- **Freeze and unfreeze:** `freeze()` only works from ACTIVE, and `unfreeze()` only works from FROZEN. Calling them in the wrong state throws an exception, so the status can only follow the allowed path ACTIVE to FROZEN to ACTIVE.
- **Close:** `close()` only works when the balance is exactly zero. After closing, the status is CLOSED and no method can debit or credit the account again.

**How the four requirements are met:**

1. **Invalid transaction amounts are rejected.** `TransactionAmount.of()` throws unless the amount is greater than zero. `Money.ofRupees()` rejects more than two decimal places. A bad amount never reaches the account.
2. **Frozen and closed accounts cannot be withdrawn from.** `requireCanDebit()` checks `status == ACTIVE` before anything else. It is the only path that reduces the balance, so there is no way around it.
3. **The overdraft limit is respected.** `OverdraftLimit.permits()` computes the lowest allowed balance (minus the limit) and the check runs inside `requireCanDebit()`, before the balance changes.
4. **Account data cannot be modified directly.** All fields are `private final` or private and have no setters. The balance can only change inside `post()`, which always writes a ledger entry at the same time. `history()` returns an unmodifiable list, and `LedgerEntry` is immutable with a package-private constructor, so outside code cannot add, remove or change entries.

The code is in `CODES/BankAccount.java`, and the demo in `CODES/Main.java` shows each rejection.

**Assumptions we made.** Deposits are still accepted into a frozen account, because a bank that freezes an account for suspected fraud still needs to receive money into it. Transfers into a frozen account are also allowed for the same reason. Only debits are blocked. Money is a single currency (INR). If the bank wants different rules, only `requireCanCredit()` needs to change.

## Question 4

**Answer:**

**Problems that arise.** Rs 5,000 is deducted from Account A and never reaches Account B. The customer loses Rs 5,000 that the bank still counts as its own. Until someone finds it, the customer cannot see the money anywhere, and the bank's books do not balance. If the customer retries the transfer, they may be charged twice.

**Business invariants violated:**
- **Invariant 6 (transfer is atomic):** only half the transfer happened.
- **Invariant 5 (balance equals the ledger):** Account A has a `TRANSFER_OUT` entry with no matching `TRANSFER_IN` on Account B. The money supply of the bank has changed without any record of why.
- In general, a transfer must not create or destroy money. The sum of all balances before and after a transfer must be equal.

**How to design the transfer to prevent money loss.** The transfer is one operation with three steps:

1. **Check everything first.** Check both accounts (A can be debited, B can be credited) and the amount. If a check fails, nothing has changed.
2. **Debit and credit inside one unit of work.** In our design, `TransferService.transfer()` runs the debit of A and the credit of B together. It is `synchronized`, so no other transfer can see a half-finished state. If the application uses a database, the debit, the credit and both ledger inserts are wrapped in one database transaction (`BEGIN ... COMMIT`). If the system crashes before `COMMIT`, the database rolls back everything, so neither side changes.
3. **Record the transfer as its own entity.** A `Transfer` object (`TRF-1`) with status PENDING, then COMPLETED or FAILED. The transfer id is written on both ledger entries, so the two halves can always be linked.

**How failures are handled.**
- *Failure before the debit:* nothing has changed, so the operation is simply rejected.
- *Failure during the credit (in our code):* the `catch` block posts a reversal entry (`TRF-2 REVERSAL`) back to A, the transfer is marked FAILED, and the customer gets an error. The history shows the debit and the reversal, so nothing is hidden. The demo shows Ravi's balance and Priya's balance unchanged after the simulated crash.
- *Crash of the whole process (in a real system):* in-memory code cannot undo changes that were not written. The fix is a database transaction, which rolls back automatically. Where the database cannot be shared, the transfer is first saved as PENDING in durable storage. A recovery job then finds PENDING transfers after a restart and either completes or reverses them. The transfer id is used as an idempotency key, so running the job twice never credits twice.

**If the two accounts belonged to different banking systems.** There is no shared database transaction across two banks, so the design changes from one atomic operation to a series of steps:
- A **saga**: Bank 1 debits A and marks the transfer PENDING. Bank 1 then sends a credit request to Bank 2 with the transfer id as an idempotency key. Bank 2 credits B and confirms. Bank 1 then marks the transfer COMPLETED.
- If Bank 2 rejects the credit (for example, B is closed), Bank 1 performs a **compensating credit** that returns the money to A and marks the transfer FAILED.
- Money is held in a **clearing or suspense account** while it is in transit, so it is always visible on the books.
- A **reconciliation job** compares the two banks' records of each transfer id each day and raises an alert for any mismatch.

The trade-off is that the system is only *eventually* consistent. For a short period money is "in transit", and the design must show that state clearly to customers and staff.

## Question 5

**Answer:**

**What a ledger entry should contain:**
- A unique entry id or sequence number within the account.
- Account number.
- Entry type (DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN, plus any reversal type).
- Amount, always positive, with the direction given by the type.
- Balance after the entry. This makes each line self-checking and lets a statement be read without re-adding the whole history.
- Date and time of posting.
- Reference, such as the transfer id, so the two halves of a transfer can be linked.
- A short description, for the customer's statement.

Our `LedgerEntry` class holds the sequence, type, amount, balance after, time and reference.

**Should an entry be modified or deleted?** No. The ledger is an audit record and banking regulations require it to be complete and unaltered. If a mistake is found, it is corrected by posting a new entry (for example a `TRANSFER_IN` reversal, like the one in Q4). The original line stays, and the correction is visible to the auditor. Editing or deleting a line would hide the original error, and it would also break the invariant that the balance equals the sum of the entries, unless the balance were edited too, which would be a second way to hide the change.

**How to keep the balance consistent with the history:**
1. **Update the balance and insert the ledger entry in the same transaction**, or in the same method, as our `post()` method does. Either both happen or neither does.
2. **Never let outside code change the balance.** The balance is private and changes only through `post()`.
3. **Check the invariant.** Our `isConsistent()` method recalculates the sum of all entries and compares it with the stored balance and the last `balanceAfter`. The demo prints `true` for both accounts. In production this would run as a nightly reconciliation job.
4. An alternative design is to derive the balance from the ledger every time. This makes a mismatch impossible but costs more to read, so a stored balance plus the check above is the usual choice.

**Challenges when the history becomes very large:**
- **Storage growth.** A busy account may have millions of entries, so storage and backup costs grow without limit.
- **Slow balance calculation.** Adding up all entries on every read becomes too slow. The fix is periodic **snapshots** of the balance (for example at each month end), so the calculation starts from the last snapshot.
- **Slow statements.** Customers expect to see a few months at a time. Indexes on (account, date) and pagination are needed.
- **Concurrency.** Many transactions on the same account can contend for the same balance row. Short transactions and careful locking (or optimistic version checks) are needed to avoid slow queues or lost updates.
- **Archiving and partitioning.** Old entries can be moved to cheaper archive storage, or the table can be partitioned by date, while keeping the audit trail complete and searchable.

## Assumptions and Notes

- All money is in Indian rupees (INR), stored in paise as a whole number, so there are no rounding errors.
- Deposits are accepted into frozen accounts. Only debits are blocked.
- The overdraft limit is per account and fixed. Interest on overdrafts is not modelled.
- The `TransferService` is `synchronized` and runs in memory to show the idea. A real banking system would use a database transaction and proper concurrency control.
- The code is in the `CODES` folder. It is in the package `casestudy3`. Run `Main.java` to see the normal operations, the rejected operations, the simulated crash and the final ledgers.
