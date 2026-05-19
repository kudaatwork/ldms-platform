import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { LDMS_PASSWORD_RULES, LdmsPasswordRule } from '@core/utils/ldms-password.util';

@Component({
  selector: 'ldms-password-requirements',
  templateUrl: './ldms-password-requirements.component.html',
  styleUrls: ['./ldms-password-requirements.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class LdmsPasswordRequirementsComponent {
  @Input() password = '';

  readonly rules: readonly LdmsPasswordRule[] = LDMS_PASSWORD_RULES;

  ruleMet(rule: LdmsPasswordRule): boolean {
    return rule.test(this.password ?? '');
  }
}
