import { Component } from '@angular/core';

@Component({
  selector: 'app-suburbs',
  template: `<app-location-table-page [entity]="'suburb'"></app-location-table-page>`,
  standalone: false,
})
export class SuburbsComponent {}
