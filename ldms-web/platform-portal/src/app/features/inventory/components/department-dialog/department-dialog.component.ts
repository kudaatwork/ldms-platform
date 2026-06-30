import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';
import { InventoryPortalService } from '../../services/inventory-portal.service';
import type { DepartmentRow } from '../../models/inventory.model';

export interface DepartmentDialogData {
  mode: 'create' | 'edit';
  department?: DepartmentRow;
}

@Component({
  selector: 'app-department-dialog',
  templateUrl: './department-dialog.component.html',
  styleUrl: './department-dialog.component.scss',
  standalone: false,
})
export class DepartmentDialogComponent {
  form: FormGroup;
  submitting = false;
  submitError = '';
  readonly isEdit: boolean;

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventoryService: InventoryPortalService,
    private readonly dialogRef: MatDialogRef<DepartmentDialogComponent, DepartmentRow>,
    @Inject(MAT_DIALOG_DATA) public readonly data: DepartmentDialogData,
  ) {
    this.dialogRef.disableClose = true;
    this.isEdit = data.mode === 'edit';
    this.form = this.buildForm(data.department);
  }

  get title(): string {
    return this.isEdit ? 'Edit department' : 'Add department';
  }

  get subtitle(): string {
    return this.isEdit
      ? 'Update the department used on purchase requisitions.'
      : 'Create a department your team can select when raising requisitions.';
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.form.get(controlName);
    return !!control && control.hasError(errorName) && (control.touched || control.dirty);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.submitError = '';
    const raw = this.form.value;
    const name = String(raw['name'] ?? '').trim();
    const departmentCode = String(raw['code'] ?? '').trim();
    const description = String(raw['description'] ?? '').trim();

    const request$ = this.isEdit
      ? this.inventoryService.updateDepartment({
          departmentId: this.data.department!.id,
          name,
          departmentCode: departmentCode || undefined,
          description: description || undefined,
        })
      : this.inventoryService.createDepartment({
          name,
          departmentCode: departmentCode || undefined,
          description: description || undefined,
        });

    request$.pipe(finalize(() => (this.submitting = false))).subscribe({
      next: (row) => this.dialogRef.close(row),
      error: (err: Error) => (this.submitError = err.message ?? 'Could not save department.'),
    });
  }

  cancel(): void {
    if (!this.submitting) {
      this.dialogRef.close();
    }
  }

  private buildForm(department?: DepartmentRow): FormGroup {
    return this.fb.group({
      name: [department?.name ?? '', [Validators.required, Validators.maxLength(200)]],
      code: [department?.code ?? '', [Validators.maxLength(60)]],
      description: [department?.description ?? '', [Validators.maxLength(500)]],
    });
  }
}
