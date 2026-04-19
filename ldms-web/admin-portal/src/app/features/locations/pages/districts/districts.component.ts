import { Component } from '@angular/core';

@Component({
  selector: 'app-districts',
  template: `<app-location-table-page [entity]="'district'"></app-location-table-page>`,
  standalone: false,
})
export class DistrictsComponent {}
