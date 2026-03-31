import { Component } from '@angular/core';

@Component({
  selector: 'app-ui-kit-demo-dialog',
  template: `
    <h2 mat-dialog-title>Confirm Dispatch Action</h2>
    <mat-dialog-content>
      <p class="dialog-copy">
        This is a reusable enterprise modal pattern for critical actions in routing, dispatch,
        and inventory workflows.
      </p>
      <div class="dialog-note">Modal surfaces support both light and dark themes.</div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" mat-dialog-close>Confirm</button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dialog-copy {
        margin: 0;
        color: var(--gray-700);
        line-height: 1.55;
      }
      .dialog-note {
        margin-top: 10px;
        font-size: 12px;
        color: var(--gray-500);
      }
    `,
  ],
  standalone: false,
})
export class UiKitDemoDialogComponent {}

