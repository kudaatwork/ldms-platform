import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { InventoryPortalService } from '../../services/inventory-portal.service';
import {
  ProductCategoryOption,
  ProductRow,
  ProductSubCategoryOption,
  PRODUCT_UNIT_OF_MEASURE_OPTIONS,
} from '../../models/inventory.model';

export interface AddProductDialogData {
  categories: ProductCategoryOption[];
  /** Owning supplier organisation id for the product catalogue. */
  supplierId: number;
  mode?: 'create' | 'edit';
  product?: ProductRow;
}

@Component({
  selector: 'app-add-product-dialog',
  templateUrl: './add-product-dialog.component.html',
  styleUrl: './add-product-dialog.component.scss',
  standalone: false,
})
export class AddProductDialogComponent implements OnInit, OnDestroy {
  form: FormGroup;
  submitting = false;
  submitError = '';
  subcategories: ProductSubCategoryOption[] = [];
  subcategoriesLoading = false;

  readonly unitOptions = PRODUCT_UNIT_OF_MEASURE_OPTIONS;
  readonly isEdit: boolean;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly inventoryService: InventoryPortalService,
    private readonly dialogRef: MatDialogRef<AddProductDialogComponent, ProductRow>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AddProductDialogData,
  ) {
    this.dialogRef.disableClose = true;
    this.isEdit = data.mode === 'edit';
    this.form = this.buildForm(data.product);
  }

  get title(): string {
    return this.isEdit ? 'Edit product' : 'Add product';
  }

  get subtitle(): string {
    return this.isEdit
      ? 'Update catalogue details for this product.'
      : 'Fill in the details below to add a new product to your catalogue.';
  }

  ngOnInit(): void {
    this.form
      .get('categoryId')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((categoryId) => this.onCategoryChanged(categoryId));

    const categoryId = this.data.product?.categoryId;
    if (categoryId) {
      this.loadSubcategories(categoryId, this.data.product?.subcategoryId);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
    const categoryId = Number(raw['categoryId']);
    if (!Number.isFinite(categoryId) || categoryId <= 0) {
      this.submitError = 'Select a product category before saving.';
      this.submitting = false;
      return;
    }

    const subcategoryId = Number(raw['subcategoryId']);
    const supplierId = this.data.product?.supplierId || this.data.supplierId;
    const subcategoryPayload =
      Number.isFinite(subcategoryId) && subcategoryId > 0 ? { subcategoryId } : {};
    const barcode = String(raw['barcode'] ?? '').trim();
    const barcodePayload = this.isEdit ? { barcode } : barcode ? { barcode } : {};

    const request$ = this.isEdit
      ? this.inventoryService.updateProduct({
          productId: this.data.product!.id,
          name: raw['name'],
          productCode: raw['code'],
          ...barcodePayload,
          description: raw['description'] ?? undefined,
          price: Number(raw['unitPrice']),
          categoryId,
          unitOfMeasure: raw['unitOfMeasure'],
          supplierId,
          ...subcategoryPayload,
        })
      : this.inventoryService.createProduct({
          name: raw['name'],
          productCode: raw['code'],
          ...barcodePayload,
          description: raw['description'] ?? undefined,
          price: Number(raw['unitPrice']),
          productCategoryId: categoryId,
          unitOfMeasure: raw['unitOfMeasure'],
          supplierId: this.data.supplierId,
          ...(Number.isFinite(subcategoryId) && subcategoryId > 0
            ? { productSubCategoryId: subcategoryId }
            : {}),
        });

    request$.pipe(finalize(() => (this.submitting = false))).subscribe({
      next: (product) => this.dialogRef.close(product),
      error: (err: Error) => (this.submitError = err.message ?? 'Could not save product.'),
    });
  }

  cancel(): void {
    if (!this.submitting) {
      this.dialogRef.close();
    }
  }

  private onCategoryChanged(categoryId: unknown): void {
    this.form.patchValue({ subcategoryId: null }, { emitEvent: false });
    this.subcategories = [];
    const id = Number(categoryId);
    if (!Number.isFinite(id) || id <= 0) {
      return;
    }
    this.loadSubcategories(id);
  }

  private loadSubcategories(categoryId: number, selectedSubcategoryId?: number): void {
    this.subcategoriesLoading = true;
    this.inventoryService
      .listSubCategoryRows()
      .pipe(
        finalize(() => (this.subcategoriesLoading = false)),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (rows) => {
          this.subcategories = rows
            .filter((row) => row.categoryId === categoryId)
            .map((row) => ({ id: row.id, categoryId: row.categoryId, name: row.name }));
          if (selectedSubcategoryId && this.subcategories.some((row) => row.id === selectedSubcategoryId)) {
            this.form.patchValue({ subcategoryId: selectedSubcategoryId }, { emitEvent: false });
          }
        },
        error: () => {
          this.subcategories = [];
        },
      });
  }

  private buildForm(product?: ProductRow): FormGroup {
    return this.fb.group({
      name: [product?.name ?? '', [Validators.required, Validators.maxLength(150)]],
      code: [product?.code ?? '', [Validators.required, Validators.maxLength(60)]],
      barcode: [product?.barcode ?? '', [Validators.maxLength(100)]],
      description: [product?.description ?? ''],
      unitPrice: [product?.unitPrice ?? 0, [Validators.required, Validators.min(0)]],
      unitOfMeasure: [product?.unitOfMeasure || 'EACH', Validators.required],
      categoryId: [product?.categoryId || null, Validators.required],
      subcategoryId: [product?.subcategoryId || null],
    });
  }
}
