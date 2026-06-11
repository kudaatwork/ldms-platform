import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/**
 * Tracks browser online/offline state via {@code navigator.onLine} and window events.
 */
@Injectable({ providedIn: 'root' })
export class NetworkConnectivityService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly online = signal(this.readInitialOnlineState());

  /** True when the browser reports a network connection. */
  readonly isOnline = this.online.asReadonly();

  constructor() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    const onOnline = () => this.online.set(true);
    const onOffline = () => this.online.set(false);
    window.addEventListener('online', onOnline);
    window.addEventListener('offline', onOffline);
  }

  private readInitialOnlineState(): boolean {
    if (typeof navigator === 'undefined') {
      return true;
    }
    return navigator.onLine;
  }
}
