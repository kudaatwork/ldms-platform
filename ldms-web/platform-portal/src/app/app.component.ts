import { Component, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { NetworkConnectivityService } from './core/services/network-connectivity.service';
import { ThemeService } from './core/services/theme.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  standalone: false,
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit, OnDestroy {
  private navSub?: Subscription;

  constructor(
    readonly connectivity: NetworkConnectivityService,
    private readonly router: Router,
    private readonly theme: ThemeService,
  ) {}

  ngOnInit(): void {
    this.theme.syncWithUrl(this.router.url);
    this.navSub = this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => this.theme.syncWithUrl(event.urlAfterRedirects));
  }

  ngOnDestroy(): void {
    this.navSub?.unsubscribe();
  }
}
