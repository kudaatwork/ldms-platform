export const SMS_DELIVERY_DISABLED_CODE = 'SMS_DELIVERY_DISABLED';

/** Thrown when phone verification cannot continue and the dialog should close. */
export class VerificationFlowError extends Error {
  constructor(
    message: string,
    readonly dismissDialog = true,
  ) {
    super(message);
    this.name = 'VerificationFlowError';
  }
}
