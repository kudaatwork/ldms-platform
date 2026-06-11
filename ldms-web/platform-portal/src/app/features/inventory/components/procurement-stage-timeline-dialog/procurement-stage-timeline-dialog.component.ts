import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { SharedModule } from '../../../../shared/shared.module';
import type { ProcurementStageStep } from '../../utils/procurement-journey.util';

export type ProcurementStageTimelineDialogData = {
  title: string;
  subtitle?: string;
  steps: ProcurementStageStep[];
};

@Component({
  selector: 'app-procurement-stage-timeline-dialog',
  templateUrl: './procurement-stage-timeline-dialog.component.html',
  styleUrl: './procurement-stage-timeline-dialog.component.scss',
  standalone: true,
  imports: [SharedModule],
})
export class ProcurementStageTimelineDialogComponent {
  constructor(
    @Inject(MAT_DIALOG_DATA) readonly data: ProcurementStageTimelineDialogData,
    private readonly dialogRef: MatDialogRef<ProcurementStageTimelineDialogComponent>,
  ) {}

  close(): void {
    this.dialogRef.close();
  }
}
