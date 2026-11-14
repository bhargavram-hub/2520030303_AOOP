package casestudy3;

// Lifecycle of a fund transfer: PENDING -> COMPLETED or PENDING -> FAILED.
// COMPLETED and FAILED are final.
public enum TransferStatus {
    PENDING,
    COMPLETED,
    FAILED
}
