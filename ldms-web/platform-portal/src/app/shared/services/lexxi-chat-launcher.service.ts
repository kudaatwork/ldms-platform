import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';

export type LexxiChatLaunchIntent = 'open' | 'toggle';
export type LexxiChatBrand = 'live-chat' | 'lexxi';

/** Opens the floating Lexxi / live chat widget from anywhere in the app. */
@Injectable({ providedIn: 'root' })
export class LexxiChatLauncherService {
  private readonly launch$ = new Subject<LexxiChatLaunchIntent>();
  private readonly openSubject = new BehaviorSubject(false);
  private readonly brandSubject = new BehaviorSubject<LexxiChatBrand>('live-chat');

  readonly launches = this.launch$.asObservable();
  readonly isOpen$ = this.openSubject.asObservable();
  readonly brand$ = this.brandSubject.asObservable();

  openChat(brand?: LexxiChatBrand): void {
    if (brand) {
      this.brandSubject.next(brand);
    }
    this.launch$.next('open');
  }

  toggleChat(): void {
    this.launch$.next('toggle');
  }

  setOpen(open: boolean): void {
    this.openSubject.next(open);
  }

  setBrand(brand: LexxiChatBrand): void {
    this.brandSubject.next(brand);
  }
}
