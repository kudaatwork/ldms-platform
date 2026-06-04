import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Matches {@code Constants.USERNAME_REGEX} in ldms-shared-library. */
export const LDMS_USERNAME_PATTERN =
  /^[a-zA-Z][a-zA-Z0-9._]{2,19}$|^[\w.%+-]+@[\w.-]+\.[a-zA-Z]{2,}$/;

export const LDMS_USERNAME_INVALID_MESSAGE =
  'Username must start with a letter and be 3–20 characters (letters, numbers, dots, underscores), or use a valid email address.';

export function isLdmsUsernameValid(username: string | null | undefined): boolean {
  const value = (username ?? '').trim();
  if (!value) {
    return false;
  }
  return LDMS_USERNAME_PATTERN.test(value);
}

export function ldmsUsernameFormatValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string | null | undefined;
    if (value == null || value === '') {
      return null;
    }
    return isLdmsUsernameValid(value) ? null : { ldmsUsernameFormat: true };
  };
}
