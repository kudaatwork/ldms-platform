import { Component } from '@angular/core';

@Component({
  selector: 'app-admin-levels',
  template: `<app-location-table-page [entity]="'admin-level'"></app-location-table-page>`,
  standalone: false,
})
export class AdminLevelsComponent {}
