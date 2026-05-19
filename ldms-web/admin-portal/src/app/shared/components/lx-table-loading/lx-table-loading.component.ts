import { Component, Input } from '@angular/core';

@Component({
  selector: 'lx-table-loading',
  templateUrl: './lx-table-loading.component.html',
  styleUrls: ['./lx-table-loading.component.scss'],
  standalone: false,
})
export class LxTableLoadingComponent {
  /** When true, shows an initial bar loader and/or an in-table overlay. */
  @Input() busy = false;
  /** When true with busy, shows a translucent overlay (place inside `.lx-table-wrap--host`). */
  @Input() hasData = false;
  @Input() message = 'Loading…';
}
