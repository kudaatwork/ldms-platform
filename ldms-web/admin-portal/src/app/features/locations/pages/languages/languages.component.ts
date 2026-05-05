import { Component } from '@angular/core';

@Component({
  selector: 'app-languages',
  template: `<app-location-table-page [entity]="'language'"></app-location-table-page>`,
  standalone: false,
})
export class LanguagesComponent {}
