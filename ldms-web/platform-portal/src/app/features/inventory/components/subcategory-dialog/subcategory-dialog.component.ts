import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';
import { InventoryPortalService } from '../../services/inventory-portal.service';
import type { ProductCategoryOption, ProductSubCategoryRow } from '../../models/inventory.model';

export interface SubcategoryDialogData {
  mode: 'create' | 'edit';
  categories: ProductCategoryOption[];
  subcategory?: ProductSubCategoryRow;
  /** Prefill parent category when adding from a filtered list. */
  defaultCategoryId?: number;
}

@Component({
  selector: 'app-subcategory-dialog',
  templateUrl: './subcategory-dialog.component.html',
  styleUrl: './subcategory-dialog.component.scss',
  standalone: false,
})
export class SubcategoryDialogComponent {
  form: FormGroup;
  submitting = false;
  submitError = '';

  readonly isEdit: boolean;

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventoryService: InventoryPortalService,
    private readonly dialogRef: MatDialogRef<SubcategoryDialogComponent, ProductSubCategoryRow>,
    @Inject(MAT_DIALOG_DATA) public readonly data: SubcategoryDialogData,
  ) {
    this.dialogRef.disableClose = true;
    this.isEdit = data.mode === 'edit';
    this.form = this.buildForm(data.subcategory, data.defaultCategoryId);
  }

  get title(): string {
    return this.isEdit ? 'Edit subcategory' : 'Add subcategory';
  }

  get subtitle(): string {
    return this.isEdit
      ? 'Update the subcategory details or move it to another category.'
      : 'Create a subcategory under a parent product category.';
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

    const categoryId = Number(this.form.value['categoryId']);
    if (!Number.isFinite(categoryId) || categoryId <= 0) {
      this.submitError = 'Select a parent category.';
      return;
    }

    this.submitting = true;
    this.submitError = '';
    const raw = this.form.value;
    const name = String(raw['name'] ?? '').trim();
    const description = String(raw['description'] ?? '').trim();

    const request$ = this.isEdit
      ? this.inventoryService.updateSubCategory({
          productSubCategoryId: this.data.subcategory!.id,
          categoryId,
          name,
          description,
        })
      : this.inventoryService.createSubCategory({ categoryId, name, description });

    request$.pipe(finalize(() => (this.submitting = false))).subscribe({
      next: (row) => this.dialogRef.close(row),
      error: (err: Error) => (this.submitError = err.message ?? 'Could not save subcategory.'),
    });
  }

  cancel(): void {
    if (!this.submitting) {
      this.dialogRef.close();
    }
  }

  private buildForm(subcategory?: ProductSubCategoryRow, defaultCategoryId?: number): FormGroup {
    return this.fb.group({
      categoryId: [subcategory?.categoryId ?? defaultCategoryId ?? null, Validators.required],
      name: [subcategory?.name ?? '', [Validators.required, Validators.maxLength(200)]],
      description: [subcategory?.description ?? '', [Validators.required, Validators.maxLength(500)]],
    });
  }
}
