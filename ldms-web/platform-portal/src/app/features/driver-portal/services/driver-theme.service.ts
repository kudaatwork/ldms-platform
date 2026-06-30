import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'lx-driver-theme';

/**
 * Theme preference for the self-contained driver portal. Independent of the main
 * app's ThemeService (which toggles `theme-dark` on <html>); here the class is
 * applied to the driver shell only. Defaults to dark (the portal is designed
 * dark + glass) but the driver can switch to light and the choice persists.
 */
@Injectable({ providedIn: 'root' })
export class DriverThemeService {
  /** When true the driver shell renders the dark + glass theme. */
  readonly dark = signal<boolean>(this.readStored());

  toggle(): void {
    this.setDark(!this.dark());
  }

  setDark(dark: boolean): void {
    this.dark.set(dark);
    try {
      localStorage.setItem(STORAGE_KEY, dark ? 'dark' : 'light');
    } catch {
      /* storage unavailable — keep in-memory preference only */
    }
  }

  private readStored(): boolean {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored === 'light') {
        return false;
      }
      if (stored === 'dark') {
        return true;
      }
    } catch {
      /* ignore */
    }
    return true; // default: dark + glass
  }
}
