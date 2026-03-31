import { Injectable } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  constructor(private readonly snackBar: MatSnackBar) {}

  show(message: string, action = 'Dismiss', config?: MatSnackBarConfig): void {
    this.snackBar.open(message, action, {
      duration: 4000,
      horizontalPosition: 'end',
      verticalPosition: 'bottom',
      ...config,
    });
  }

  error(message: string): void {
    this.show(message, 'Close', { panelClass: ['app-snackbar-error'] });
  }

  success(message: string): void {
    this.show(message, 'OK', { panelClass: ['app-snackbar-success'] });
  }
}
