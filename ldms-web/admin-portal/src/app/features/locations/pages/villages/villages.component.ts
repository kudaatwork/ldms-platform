import { Component } from '@angular/core';

@Component({
  selector: 'app-villages',
  template: `<app-location-table-page [entity]="'village'"></app-location-table-page>`,
  standalone: false,
})
export class VillagesComponent {}
