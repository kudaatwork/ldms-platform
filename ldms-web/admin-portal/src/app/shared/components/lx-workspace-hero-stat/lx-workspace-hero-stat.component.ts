import { Component, Input } from '@angular/core';

export type LxWorkspaceHeroStatTheme = 'teal' | 'mint' | 'amber' | 'violet';

/**
 * LxWorkspaceHeroStatComponent
 *
 * Purpose: A single bento-style stat card projected into LxWorkspaceHeroComponent.
 *
 * Usage (inside <lx-workspace-hero>):
 * ```html
 * <lx-workspace-hero-stat
 *   value="12"
 *   label="Total vehicles"
 *   icon="garage"
 *   theme="teal"
 * ></lx-workspace-hero-stat>
 * ```
 *
 * Themes: 'teal' (default) | 'mint' | 'amber' | 'violet'
 */
@Component({
  selector: 'lx-workspace-hero-stat',
  templateUrl: './lx-workspace-hero-stat.component.html',
  styleUrls: ['./lx-workspace-hero-stat.component.scss'],
  standalone: false,
})
export class LxWorkspaceHeroStatComponent {
  /** Numeric or formatted string value displayed prominently. */
  @Input() value: string | number = '';

  /** Short descriptor label below the value. */
  @Input() label = '';

  /** Optional mat-icon name rendered as a background accent icon. */
  @Input() icon?: string;

  /** Colour theme for the card background gradient. Default: 'teal'. */
  @Input() theme: LxWorkspaceHeroStatTheme = 'teal';
}
