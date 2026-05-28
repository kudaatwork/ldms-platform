/** Minimum age for date-of-birth fields (contact persons). */
export const MINIMUM_DATE_OF_BIRTH_AGE_YEARS = 18;

/** Latest allowed ISO date (`yyyy-MM-dd`) for a date-of-birth input (turned 18 today or earlier). */
export function maximumDateOfBirthInput(referenceDate: Date = new Date()): string {
  const maxDate = new Date(referenceDate.getTime());
  maxDate.setFullYear(maxDate.getFullYear() - MINIMUM_DATE_OF_BIRTH_AGE_YEARS);
  return toIsoDateInputValue(maxDate);
}

/** True when `rawDate` is a valid ISO date and the person is at least {@link MINIMUM_DATE_OF_BIRTH_AGE_YEARS}. */
export function isDateOfBirthAtLeastMinimumAge(
  rawDate: string,
  referenceDate: Date = new Date(),
): boolean {
  const trimmed = rawDate.trim();
  if (!trimmed) {
    return false;
  }
  const picked = new Date(trimmed);
  if (Number.isNaN(picked.getTime())) {
    return false;
  }
  return trimmed <= maximumDateOfBirthInput(referenceDate);
}

export function dateOfBirthMinimumAgeMessage(): string {
  return `Date of birth must be at least ${MINIMUM_DATE_OF_BIRTH_AGE_YEARS} years ago.`;
}

function toIsoDateInputValue(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
