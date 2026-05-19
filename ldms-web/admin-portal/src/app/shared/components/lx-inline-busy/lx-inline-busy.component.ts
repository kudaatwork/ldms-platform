import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'lx-inline-busy',
  templateUrl: './lx-inline-busy.component.html',
  styleUrls: ['./lx-inline-busy.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class LxInlineBusyComponent {
  @Input() message = 'Please wait…';
  @Input() diameter = 18;
}
