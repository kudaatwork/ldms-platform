import { Directive } from '@angular/core';

/**
 * LxWorkspaceHeroActionsDirective
 *
 * Marks a host element for projection into the action-button slot of
 * {@link LxWorkspaceHeroComponent}.
 *
 * Usage:
 * ```html
 * <lx-workspace-hero ...>
 *   <div lxWorkspaceHeroActions>
 *     <button class="lx-btn lx-btn-ghost lx-btn-sm">Refresh</button>
 *     <button class="lx-btn lx-btn-primary lx-btn-sm lx-ws-hero__btn--accent">Add</button>
 *   </div>
 * </lx-workspace-hero>
 * ```
 */
@Directive({
  selector: '[lxWorkspaceHeroActions]',
  standalone: false,
})
export class LxWorkspaceHeroActionsDirective {}
