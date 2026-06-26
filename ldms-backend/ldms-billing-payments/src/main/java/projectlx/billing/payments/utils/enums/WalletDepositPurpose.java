package projectlx.billing.payments.utils.enums;

/**
 * What an organisation's payment (with proof) is for.
 * WALLET_TOPUP credits the prepaid wallet on approval; SUBSCRIPTION activates a
 * monthly subscription package on approval.
 */
public enum WalletDepositPurpose {
    WALLET_TOPUP,
    SUBSCRIPTION
}
