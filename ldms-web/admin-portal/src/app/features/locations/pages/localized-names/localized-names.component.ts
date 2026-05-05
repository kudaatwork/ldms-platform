import { Component } from '@angular/core';

@Component({
  selector: 'app-localized-names',
  template: `<app-location-table-page [entity]="'localized-name'"></app-location-table-page>`,
  standalone: false,
})
export class LocalizedNamesComponent {}
