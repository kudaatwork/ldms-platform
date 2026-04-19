import { Component } from '@angular/core';

@Component({
  selector: 'app-cities',
  template: `<app-location-table-page [entity]="'city'"></app-location-table-page>`,
  standalone: false,
})
export class CitiesComponent {}
