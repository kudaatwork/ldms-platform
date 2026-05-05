import { Component } from '@angular/core';

@Component({
  selector: 'app-addresses',
  template: `<app-location-table-page [entity]="'address'"></app-location-table-page>`,
  standalone: false,
})
export class AddressesComponent {}
