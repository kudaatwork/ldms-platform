import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs/operators';

@Component({
  selector: 'app-inventory-integration-subnav',
  templateUrl: './inventory-integration-subnav.component.html',
  styleUrl: './inventory-integration-subnav.component.scss',
  standalone: false,
})
export class InventoryIntegrationSubnavComponent {
  private readonly route = inject(ActivatedRoute);

  readonly docQueryParams$ = this.route.queryParamMap.pipe(
    map((params) => ({ mode: params.get('mode') === 'crossdock' ? 'crossdock' : 'inventory' })),
  );
}
