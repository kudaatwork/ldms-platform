import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-users-hub-nav',
  templateUrl: './users-hub-nav.component.html',
  styleUrl: './users-hub-nav.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class UsersHubNavComponent {
  constructor(private readonly router: Router) {}

  /** Re-run the users list route when "All users" is clicked while already on `/users`. */
  navigateAllUsers(event: Event): void {
    const path = this.router.url.split('?')[0].split('#')[0];
    if (path !== '/users') {
      return;
    }
    event.preventDefault();
    void this.router.navigateByUrl('/users', { onSameUrlNavigation: 'reload' });
  }
}
