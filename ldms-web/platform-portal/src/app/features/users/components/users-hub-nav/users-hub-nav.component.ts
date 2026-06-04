import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-users-hub-nav',
  templateUrl: './users-hub-nav.component.html',
  styleUrl: './users-hub-nav.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class UsersHubNavComponent {}
