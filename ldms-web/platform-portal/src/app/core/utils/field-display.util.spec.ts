import {
  formatIsoDateForDisplay,
  formatSecurityQuestionLabel,
  resolveUserRoleLabel,
  shellRoleSummary,
} from './field-display.util';

describe('field-display.util', () => {
  it('formats ISO dates for display', () => {
    expect(formatIsoDateForDisplay('1993-06-15T22:00:00.000+00:00')).not.toContain('T22:00:00');
  });

  it('resolves role label from user group', () => {
    expect(
      resolveUserRoleLabel({
        userGroupDto: { name: 'Administrator' },
      }),
    ).toBe('Administrator');
  });

  it('prefers role label over org classification in shell summary', () => {
    expect(shellRoleSummary('Administrator', 'SUPPLIER')).toBe('Administrator');
    expect(shellRoleSummary('', 'SUPPLIER')).toBe('SUPPLIER');
  });

  it('maps security question placeholders to Not set', () => {
    expect(formatSecurityQuestionLabel('Please set your first security question (profile).')).toBe(
      'Not set',
    );
  });
});
