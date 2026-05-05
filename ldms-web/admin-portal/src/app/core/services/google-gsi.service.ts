import { Injectable, NgZone } from '@angular/core';
import { environment } from '../../../environments/environment';

const GSI_SCRIPT_ID = 'ldms-google-gsi-client';

/** Google Identity Services loaded at runtime (avoid augmenting `Window` — conflicts with Places typings). */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
type GsiWindow = Window & { google?: any };

@Injectable({ providedIn: 'root' })
export class GoogleGsiService {
  private scriptPromise: Promise<void> | null = null;

  constructor(private readonly zone: NgZone) {}

  get clientId(): string {
    return environment.googleOAuthClientId?.trim() ?? '';
  }

  loadScript(): Promise<void> {
    if (!this.clientId) {
      return Promise.reject(new Error('Google OAuth client ID is not configured.'));
    }
    const w = window as GsiWindow;
    if (w.google?.accounts?.id) {
      return Promise.resolve();
    }
    if (this.scriptPromise) {
      return this.scriptPromise;
    }
    this.scriptPromise = new Promise((resolve, reject) => {
      const existing = document.getElementById(GSI_SCRIPT_ID) as HTMLScriptElement | null;
      if (existing?.dataset['loaded'] === 'true' && (window as GsiWindow).google?.accounts?.id) {
        resolve();
        return;
      }
      const s = existing ?? document.createElement('script');
      s.id = GSI_SCRIPT_ID;
      s.src = 'https://accounts.google.com/gsi/client';
      s.async = true;
      s.defer = true;
      s.onload = () => {
        s.dataset['loaded'] = 'true';
        resolve();
      };
      s.onerror = () => reject(new Error('Failed to load Google sign-in script.'));
      if (!existing) {
        document.head.appendChild(s);
      }
    });
    return this.scriptPromise;
  }

  renderSignInButton(host: HTMLElement, onCredential: (idToken: string) => void): void {
    const id = this.clientId;
    const w = window as GsiWindow;
    const gsi = w.google?.accounts?.id;
    if (!id || !gsi) {
      return;
    }
    host.innerHTML = '';
    gsi.initialize({
      client_id: id,
      callback: (resp: { credential: string }) => {
        this.zone.run(() => onCredential(resp.credential));
      },
      auto_select: false,
      cancel_on_tap_outside: true,
    });
    gsi.renderButton(host, {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      text: 'continue_with',
      shape: 'rectangular',
      logo_alignment: 'left',
      width: 280,
    });
  }
}
