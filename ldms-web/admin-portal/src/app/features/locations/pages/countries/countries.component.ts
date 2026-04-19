import { Component } from '@angular/core';

@Component({
  selector: 'app-countries',
  template: `<app-location-table-page [entity]="'country'"></app-location-table-page>`,
  standalone: false,
})
export class CountriesComponent {}
