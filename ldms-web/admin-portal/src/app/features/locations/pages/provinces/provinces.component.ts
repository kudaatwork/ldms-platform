import { Component } from '@angular/core';

@Component({
  selector: 'app-provinces',
  template: `<app-location-table-page [entity]="'province'"></app-location-table-page>`,
  standalone: false,
})
export class ProvincesComponent {}
