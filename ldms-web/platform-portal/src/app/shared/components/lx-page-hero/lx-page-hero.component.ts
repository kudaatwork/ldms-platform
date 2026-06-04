import { Component, Input } from '@angular/core';

export type LxPageHeroVariant = 'brand' | 'slate' | 'violet' | 'emerald' | 'amber' | 'rose';

@Component({
  selector: 'lx-page-hero',
  templateUrl: './lx-page-hero.component.html',
  styleUrl: './lx-page-hero.component.scss',
  standalone: false,
})
export class LxPageHeroComponent {
  @Input() eyebrow = '';
  @Input() title = '';
  @Input() lead = '';
  @Input() icon = '';
  @Input() variant: LxPageHeroVariant = 'brand';
}
