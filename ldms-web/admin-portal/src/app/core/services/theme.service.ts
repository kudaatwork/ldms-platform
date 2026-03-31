import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

const STORAGE_KEY = 'lx-admin-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);

  /** When true, `theme-dark` is applied on the document root. */
  readonly dark = signal(false);

  initFromStorage(): void {
    const stored = localStorage.getItem(STORAGE_KEY);
    let useDark = false;
    if (stored === 'dark') {
      useDark = true;
    } else if (stored === 'light') {
      useDark = false;
    } else {
      useDark =
        this.document.defaultView?.matchMedia('(prefers-color-scheme: dark)')
          .matches ?? false;
    }
    this.setDarkClass(useDark);
  }

  toggle(): void {
    const next = !this.dark();
    this.setDarkClass(next);
    localStorage.setItem(STORAGE_KEY, next ? 'dark' : 'light');
  }

  private setDarkClass(dark: boolean): void {
    this.dark.set(dark);
    this.document.documentElement.classList.toggle('theme-dark', dark);
  }
}
