import { Directive, OnInit, inject } from '@angular/core';
import { MatPaginator, MatPaginatorSelectConfig } from '@angular/material/paginator';

const PANEL_CLASS = 'lx-table-paginator-select-panel';

function mergePanelClass(
  existing: MatPaginatorSelectConfig['panelClass'] | undefined,
  extra: string,
): MatPaginatorSelectConfig['panelClass'] {
  if (!existing) {
    return extra;
  }
  if (typeof existing === 'string') {
    return existing === extra ? existing : [existing, extra];
  }
  if (Array.isArray(existing)) {
    return existing.includes(extra) ? existing : [...existing, extra];
  }
  if (existing instanceof Set) {
    if (existing.has(extra)) {
      return existing;
    }
    return new Set([...existing, extra]);
  }
  return extra;
}

/**
 * Applies shared page-size select behaviour for {@link MatPaginator} when the host uses
 * {@code class="lx-table-paginator"} (panel class + option centering).
 *
 * Does not overwrite {@link MatPaginator#pageSize}. Parents bind {@code [pageSize]} to match
 * server requests; forcing a default here emitted {@code (page)}, caused double loads, and
 * load-token races that left tables empty (e.g. Users list at page size 10).
 */
@Directive({
  selector: 'mat-paginator.lx-table-paginator',
  standalone: true,
})
export class LxTablePaginatorDirective implements OnInit {
  private readonly paginator = inject(MatPaginator, { self: true });

  ngOnInit(): void {
    const c = this.paginator.selectConfig ?? {};
    this.paginator.selectConfig = {
      ...c,
      panelClass: mergePanelClass(c.panelClass, PANEL_CLASS),
      disableOptionCentering: c.disableOptionCentering ?? true,
    };
  }
}
