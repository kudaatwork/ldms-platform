import { Directive } from '@angular/core';

/** Marks content for projection into {@link LxPageHeroComponent} action slot. */
@Directive({
  selector: '[lxPageHeroActions]',
  standalone: false,
})
export class LxPageHeroActionsDirective {}
