import { Injectable, NgZone } from '@angular/core';
import { environment } from '../../../environments/environment';

const GSI_SCRIPT_ID = 'ldms-google-gsi-client-platform';

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
      const s = document.getElementById(GSI_SCRIPT_ID) as HTMLScriptElement | null;
      if (s?.dataset['loaded'] === 'true' && (window as GsiWindow).google?.accounts?.id) {
        resolve();
        return;
      }
      const el = s ?? document.createElement('script');
      el.id = GSI_SCRIPT_ID;
      el.src = 'https://accounts.google.com/gsi/client';
      el.async = true;
      el.defer = true;
      el.onload = () => {
        el.dataset['loaded'] = 'true';
        resolve();
      };
      el.onerror = () => reject(new Error('Failed to load Google sign-in script.'));
      if (!s) {
        document.head.appendChild(el);
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
