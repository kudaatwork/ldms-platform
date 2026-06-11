package projectlx.billing.payments.utils.enums;

/**
 * How payment proof entered the platform. System-recorded and uploaded proofs carry equal weight;
 * both require amount, reference, and date metadata.
 */
public enum PaymentProofSource {
    SYSTEM_GENERATED,
    EXTERNAL_UPLOAD
}
