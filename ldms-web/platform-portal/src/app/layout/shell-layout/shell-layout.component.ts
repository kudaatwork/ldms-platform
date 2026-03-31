import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CurrentUser } from '../../core/models/auth.model';
import { AuthService } from '../../core/services/auth.service';
import { AuthStateService } from '../../core/services/auth-state.service';
import { NAV_CONFIG, NavItem } from '../sidebar/sidebar.config';

@Component({
  selector: 'app-shell-layout',
  templateUrl: './shell-layout.component.html',
  styleUrls: ['./shell-layout.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ShellLayoutComponent implements OnInit {
  currentUser: CurrentUser | null = null;
  navItems: NavItem[] = [];

  constructor(
    private readonly authService: AuthService,
    private readonly authState: AuthStateService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authState.currentUser;
    if (!this.currentUser) {
      this.authService.bootstrapFromStorage();
      this.currentUser = this.authState.currentUser;
    }
    this.navItems = this.currentUser ? NAV_CONFIG[this.currentUser.orgClassification] : [];
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/auth/login']);
  }
}
