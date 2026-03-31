import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { CurrentUser } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly userSubject = new BehaviorSubject<CurrentUser | null>(null);
  readonly currentUser$ = this.userSubject.asObservable();

  get currentUser(): CurrentUser | null {
    return this.userSubject.value;
  }

  setCurrentUser(user: CurrentUser | null): void {
    this.userSubject.next(user);
  }
}
