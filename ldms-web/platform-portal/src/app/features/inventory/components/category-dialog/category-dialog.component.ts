import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';
import { InventoryPortalService } from '../../services/inventory-portal.service';
import type { ProductCategoryRow } from '../../models/inventory.model';

export interface CategoryDialogData {
  mode: 'create' | 'edit';
  category?: ProductCategoryRow;
}

@Component({
  selector: 'app-category-dialog',
  templateUrl: './category-dialog.component.html',
  styleUrl: './category-dialog.component.scss',
  standalone: false,
})
export class CategoryDialogComponent {
  form: FormGroup;
  submitting = false;
  submitError = '';

  readonly isEdit: boolean;

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventoryService: InventoryPortalService,
    private readonly dialogRef: MatDialogRef<CategoryDialogComponent, ProductCategoryRow>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CategoryDialogData,
  ) {
    this.dialogRef.disableClose = true;
    this.isEdit = data.mode === 'edit';
    this.form = this.buildForm(data.category);
  }

  get title(): string {
    return this.isEdit ? 'Edit category' : 'Add category';
  }

  get subtitle(): string {
    return this.isEdit
      ? 'Update the category name or description.'
      : 'Create a top-level product category for your catalogue.';
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
    const description = String(raw['description'] ?? '').trim();

    const request$ = this.isEdit
      ? this.inventoryService.updateCategory({
          productCategoryId: this.data.category!.id,
          name,
          description,
        })
      : this.inventoryService.createCategory({ name, description });

    request$.pipe(finalize(() => (this.submitting = false))).subscribe({
      next: (row) => this.dialogRef.close(row),
      error: (err: Error) => (this.submitError = err.message ?? 'Could not save category.'),
    });
  }

  cancel(): void {
    if (!this.submitting) {
      this.dialogRef.close();
    }
  }

  private buildForm(category?: ProductCategoryRow): FormGroup {
    return this.fb.group({
      name: [category?.name ?? '', [Validators.required, Validators.maxLength(200)]],
      description: [category?.description ?? '', [Validators.required, Validators.maxLength(500)]],
    });
  }
}
