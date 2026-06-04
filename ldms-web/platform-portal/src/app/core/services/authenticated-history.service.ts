import { Injectable, OnDestroy } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { StorageService } from './storage.service';

/**
 * Prevents signed-in users from using the browser Back button to return to login / welcome pages.
 */
@Injectable({ providedIn: 'root' })
export class AuthenticatedHistoryService implements OnDestroy {
  private armed = false;
  private popListener: (() => void) | null = null;
  private routerSub: Subscription | null = null;
  private workspaceUrl = '';

  constructor(
    private readonly router: Router,
    private readonly storage: StorageService,
  ) {}

  ngOnDestroy(): void {
    this.disable();
  }

  enable(workspaceUrl: string): void {
    if (this.armed) {
      return;
    }
    this.armed = true;
    this.workspaceUrl = workspaceUrl || this.router.url;

    history.pushState({ lxAuthenticatedShell: true }, '', window.location.href);

    this.popListener = () => this.onPopState();
    window.addEventListener('popstate', this.popListener);

    this.routerSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => this.guardAuthenticatedRoute(e.urlAfterRedirects));
  }

  disable(): void {
    this.armed = false;
    this.workspaceUrl = '';
    if (this.popListener) {
      window.removeEventListener('popstate', this.popListener);
      this.popListener = null;
    }
    this.routerSub?.unsubscribe();
    this.routerSub = null;
  }

  private onPopState(): void {
    if (!this.isSignedIn()) {
      return;
    }
    const path = `${window.location.pathname}${window.location.search}`;
    if (this.isPreAuthPath(path)) {
      history.pushState({ lxAuthenticatedShell: true }, '', window.location.href);
      void this.router.navigateByUrl(this.workspaceUrl || '/dashboard', { replaceUrl: true });
    }
  }

  private guardAuthenticatedRoute(url: string): void {
    if (!this.isSignedIn()) {
      return;
    }
    const path = url.split('#')[0];
    if (this.isPreAuthPath(path)) {
      void this.router.navigateByUrl(this.workspaceUrl || '/dashboard', { replaceUrl: true });
    }
  }

  private isSignedIn(): boolean {
    const token = this.storage.getToken();
    return Boolean(token && !token.startsWith('mock.'));
  }

  private isPreAuthPath(path: string): boolean {
    const p = path.split('?')[0];
    return (
      p === '/welcome' ||
      p === '/contact' ||
      p.startsWith('/auth') ||
      p.startsWith('/signup') ||
      p === '/'
    );
  }
}
