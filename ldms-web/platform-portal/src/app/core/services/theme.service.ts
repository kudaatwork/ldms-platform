import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

const STORAGE_KEY = 'lx-platform-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);

  /** When true, `theme-dark` is applied on the document root. */
  readonly dark = signal(false);

  /** True while the public landing route is active (theme toggle does not persist). */
  readonly onLanding = signal(false);

  initFromStorage(): void {
    this.syncWithUrl(this.currentPath());
  }

  /** Apply landing-dark vs app theme whenever navigation completes. */
  syncWithUrl(url: string): void {
    const path = this.normalizePath(url);
    const landing = this.isLandingPath(path);
    this.onLanding.set(landing);
    if (landing) {
      this.setDarkClass(true);
      return;
    }
    this.applyAppTheme();
  }

  toggle(): void {
    const next = !this.dark();
    this.setDarkClass(next);
    if (!this.onLanding()) {
      localStorage.setItem(STORAGE_KEY, next ? 'dark' : 'light');
    }
  }

  private applyAppTheme(): void {
    const stored = localStorage.getItem(STORAGE_KEY);
    let useDark = false;
    if (stored === 'dark') {
      useDark = true;
    } else if (stored === 'light') {
      useDark = false;
    } else {
      // App workspace defaults to light unless the user explicitly chose dark.
      useDark = false;
    }
    this.setDarkClass(useDark);
  }

  private setDarkClass(dark: boolean): void {
    this.dark.set(dark);
    this.document.documentElement.classList.toggle('theme-dark', dark);
  }

  private currentPath(): string {
    return this.document.defaultView?.location.pathname ?? '/';
  }

  private normalizePath(url: string): string {
    const path = url.split('?')[0]?.split('#')[0] ?? '/';
    return path.replace(/\/+$/, '') || '/';
  }

  private isLandingPath(path: string): boolean {
    const normalized = this.normalizePath(path);
    return normalized === '/welcome' || normalized === '/';
  }
}
