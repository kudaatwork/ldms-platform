import { Component, Input } from '@angular/core';

export type LxWorkspaceHeroVariant = 'teal' | 'brand' | 'violet' | 'emerald' | 'amber' | 'rose' | 'slate';

/**
 * LxWorkspaceHeroComponent
 *
 * Purpose: Reusable workspace-page hero banner with animated background,
 *          orbital icon widget, action-button slot, and projected bento stats.
 *
 * Usage:
 * ```html
 * <lx-workspace-hero
 *   eyebrow="Corridor intelligence"
 *   eyebrowIcon="hub"
 *   title="Fleet & Transporters"
 *   lead="Manage your own-fleet and contracted transporters."
 *   hubIcon="local_shipping"
 *   [orbitIcons]="['route','verified','speed']"
 *   note="Optional preview note shown below action buttons."
 * >
 *   <div lxWorkspaceHeroActions>
 *     <button class="lx-btn lx-btn-ghost lx-btn-sm">Refresh</button>
 *   </div>
 *   <lx-workspace-hero-stat value="12" label="Total vehicles"   icon="garage"    theme="teal"></lx-workspace-hero-stat>
 *   <lx-workspace-hero-stat value="4"  label="On corridor"     icon="near_me"   theme="mint"></lx-workspace-hero-stat>
 *   <lx-workspace-hero-stat value="78%" label="Avg utilisation" icon="insights"  theme="amber"></lx-workspace-hero-stat>
 *   <lx-workspace-hero-stat value="3"  label="Partners"        icon="handshake" theme="violet"></lx-workspace-hero-stat>
 * </lx-workspace-hero>
 * ```
 *
 * Content slots:
 *   [lxWorkspaceHeroActions] — projected into the action-button row
 *   lx-workspace-hero-stat   — projected into the bento stat grid
 */
@Component({
  selector: 'lx-workspace-hero',
  templateUrl: './lx-workspace-hero.component.html',
  styleUrls: ['./lx-workspace-hero.component.scss'],
  standalone: false,
})
export class LxWorkspaceHeroComponent {
  /** Pill label above the title. */
  @Input() eyebrow = '';

  /** Mat-icon name shown inside the eyebrow pill. Default: 'hub'. */
  @Input() eyebrowIcon = 'hub';

  /** Main heading of the hero. */
  @Input() title = '';

  /** Supporting paragraph below the title. */
  @Input() lead = '';

  /** Mat-icon displayed in the centre of the orbit widget. Default: 'insights'. */
  @Input() hubIcon = 'insights';

  /** Up to 3 mat-icon names for the three orbit nodes. Default: route, verified, speed. */
  @Input() orbitIcons: string[] = ['route', 'verified', 'speed'];

  /**
   * Optional preview/informational note rendered below the action buttons
   * with an auto_awesome icon prefix. Omit or pass empty string to hide.
   */
  @Input() note?: string;

  /** Colour theme for the hero shell. Default: 'teal' (Fleet / corridor). */
  @Input() variant: LxWorkspaceHeroVariant = 'teal';
}
