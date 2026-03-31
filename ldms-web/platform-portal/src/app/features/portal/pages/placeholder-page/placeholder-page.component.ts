import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-placeholder-page',
  templateUrl: './placeholder-page.component.html',
  styleUrls: ['./placeholder-page.component.scss'],
  standalone: false,
})
export class PlaceholderPageComponent {
  constructor(readonly route: ActivatedRoute) {}
}
