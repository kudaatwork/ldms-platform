import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Matches {@code Constants.PASSWORD_REGEX} in ldms-shared-library. */
export const LDMS_PASSWORD_PATTERN =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#])[A-Za-z\d@$!%*?&#]{8,20}$/;

export const LDMS_PASSWORD_INVALID_MESSAGE =
  'Password must be 8–20 characters and include uppercase, lowercase, a digit, and one special character (@ $ ! % * ? & #).';

export interface LdmsPasswordRule {
  id: string;
  label: string;
  test: (password: string) => boolean;
}

export const LDMS_PASSWORD_RULES: readonly LdmsPasswordRule[] = [
  { id: 'length', label: '8–20 characters', test: (p) => p.length >= 8 && p.length <= 20 },
  { id: 'lower', label: 'One lowercase letter', test: (p) => /[a-z]/.test(p) },
  { id: 'upper', label: 'One uppercase letter', test: (p) => /[A-Z]/.test(p) },
  { id: 'digit', label: 'One digit', test: (p) => /\d/.test(p) },
  { id: 'special', label: 'One special character (@ $ ! % * ? & #)', test: (p) => /[@$!%*?&#]/.test(p) },
  {
    id: 'charset',
    label: 'Only letters, digits, and @ $ ! % * ? & #',
    test: (p) => p.length === 0 || /^[A-Za-z\d@$!%*?&#]+$/.test(p),
  },
] as const;

export function isLdmsPasswordValid(password: string | null | undefined): boolean {
  const value = (password ?? '').trim();
  if (!value) {
    return false;
  }
  return LDMS_PASSWORD_PATTERN.test(value);
}

export function ldmsPasswordFormatValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string | null | undefined;
    if (value == null || value === '') {
      return null;
    }
    return isLdmsPasswordValid(value) ? null : { ldmsPasswordFormat: true };
  };
}
