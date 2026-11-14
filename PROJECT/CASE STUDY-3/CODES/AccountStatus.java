package casestudy3;

// The closed set of states an account can be in.
// ACTIVE: normal use. FROZEN: no debits (deposits are still accepted).
// CLOSED: final state, no debits and no credits.
public enum AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED
}
