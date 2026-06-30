import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  Output,
  ViewChild,
  forwardRef,
} from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  MatAutocompleteModule,
  MatAutocompleteSelectedEvent,
  MatAutocompleteTrigger,
} from '@angular/material/autocomplete';
import { MatIconModule } from '@angular/material/icon';
import { LinkedSupplierOption } from '../../models/inventory.model';
import {
  filterSuppliersForSearch,
  supplierPickerLabel,
  supplierSearchText,
} from '../../utils/supplier-search.util';

export type SupplierStockHintTone = 'ok' | 'warn' | 'muted';

@Component({
  selector: 'app-searchable-supplier-picker',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatAutocompleteModule],
  templateUrl: './searchable-supplier-picker.component.html',
  styleUrl: './searchable-supplier-picker.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SearchableSupplierPickerComponent),
      multi: true,
    },
  ],
})
export class SearchableSupplierPickerComponent implements ControlValueAccessor {
  private static nextId = 0;
  private readonly instanceId = ++SearchableSupplierPickerComponent.nextId;

  @ViewChild(MatAutocompleteTrigger) private autocompleteTrigger?: MatAutocompleteTrigger;

  @Input() suppliers: LinkedSupplierOption[] = [];
  @Input() label = 'Preferred supplier';
  @Input() required = false;
  @Input() invalid = false;
  @Input() disabled = false;
  @Input() searchPlaceholder = 'Search supplier by name or email…';
  @Input() inputId = '';
  @Input() ariaLabel = 'Preferred supplier';
  @Input() emptyHint = '';
  /** Optional per-supplier stock/catalog hint (e.g. "Has your product in stock"). */
  @Input() stockHintBySupplierId: Readonly<Record<number, string>> | null = null;
  @Input() stockHintToneBySupplierId: Readonly<Record<number, SupplierStockHintTone>> | null = null;

  @Output() selectionChange = new EventEmitter<number | null>();

  searchText = '';
  value: number | null = null;

  private onChange: (value: number | null) => void = () => {};
  private onTouched: () => void = () => {};

  get inputElementId(): string {
    return this.inputId || `supplier-picker-input-${this.instanceId}`;
  }

  get searchQuery(): string {
    return this.asSearchString(this.searchText);
  }

  get filteredSuppliers(): LinkedSupplierOption[] {
    return filterSuppliersForSearch(this.suppliers, this.searchQuery, this.stockHintBySupplierId);
  }

  supplierLabel(s: LinkedSupplierOption): string {
    return supplierPickerLabel(s);
  }

  supplierOptionValue(s: LinkedSupplierOption): string {
    return this.supplierLabel(s);
  }

  supplierMeta(s: LinkedSupplierOption): string {
    const stockHint = this.stockHintBySupplierId?.[s.id]?.trim();
    if (stockHint) {
      return stockHint;
    }
    return s.email?.trim() ?? '';
  }

  supplierMetaTone(s: LinkedSupplierOption): SupplierStockHintTone {
    return this.stockHintToneBySupplierId?.[s.id] ?? 'muted';
  }

  supplierSearchBlob(s: LinkedSupplierOption): string {
    const stockHint = this.stockHintBySupplierId?.[s.id] ?? '';
    return `${supplierSearchText(s)} ${stockHint}`.trim();
  }

  onInputFocus(): void {
    if (this.value != null) {
      this.searchText = this.labelForId(this.value);
    }
    setTimeout(() => this.autocompleteTrigger?.openPanel());
  }

  onSearchInput(): void {
    this.normaliseSearchText();
    if (this.restoreLabelWhenInputMatchesSelectedId()) {
      return;
    }
    if (this.coerceNumericInputToSupplierLabel()) {
      return;
    }
    const selectedLabel = this.labelForId(this.value);
    if (this.value != null && this.searchQuery !== selectedLabel) {
      this.value = null;
      this.onChange(null);
      this.selectionChange.emit(null);
    }
    setTimeout(() => this.autocompleteTrigger?.openPanel());
  }

  onSupplierPicked(event: MatAutocompleteSelectedEvent): void {
    const supplier = this.supplierFromOptionValue(event.option.value);
    if (!supplier) {
      return;
    }
    this.applySupplierSelection(supplier);
    this.autocompleteTrigger?.closePanel();
  }

  onBlur(): void {
    this.onTouched();
    this.normaliseSearchText();
    this.restoreLabelWhenInputMatchesSelectedId();
    this.coerceNumericInputToSupplierLabel();
    const selectedLabel = this.labelForId(this.value);
    if (this.value != null && this.searchQuery !== selectedLabel) {
      this.searchText = selectedLabel;
    }
  }

  writeValue(value: number | null): void {
    this.value = value;
    this.searchText = this.labelForId(value);
  }

  registerOnChange(fn: (value: number | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  private asSearchString(raw: unknown): string {
    if (raw == null || typeof raw === 'object') {
      return '';
    }
    return String(raw).trim();
  }

  private normaliseSearchText(): void {
    this.searchText = this.asSearchString(this.searchText);
  }

  private labelForId(id: number | null): string {
    if (id == null) {
      return '';
    }
    const supplier = this.suppliers.find((s) => s.id === id);
    return supplier ? this.supplierLabel(supplier) : '';
  }

  private supplierFromOptionValue(optionValue: unknown): LinkedSupplierOption | null {
    const raw = this.asSearchString(optionValue);
    if (!raw) {
      return null;
    }
    if (/^\d+$/.test(raw)) {
      const id = Number(raw);
      return this.suppliers.find((s) => s.id === id) ?? null;
    }
    const byLabel = this.suppliers.filter((s) => this.supplierLabel(s) === raw);
    if (byLabel.length === 1) {
      return byLabel[0];
    }
    if (byLabel.length > 1 && this.value != null) {
      return byLabel.find((s) => s.id === this.value) ?? byLabel[0];
    }
    return byLabel[0] ?? null;
  }

  private restoreLabelWhenInputMatchesSelectedId(): boolean {
    if (this.value == null || this.searchQuery !== String(this.value)) {
      return false;
    }
    this.searchText = this.labelForId(this.value);
    return true;
  }

  private coerceNumericInputToSupplierLabel(): boolean {
    if (!/^\d+$/.test(this.searchQuery)) {
      return false;
    }
    const supplier = this.suppliers.find((s) => s.id === Number(this.searchQuery));
    if (!supplier) {
      return false;
    }
    this.applySupplierSelection(supplier);
    return true;
  }

  private applySupplierSelection(supplier: LinkedSupplierOption): void {
    this.value = supplier.id;
    this.searchText = this.supplierLabel(supplier);
    this.onChange(supplier.id);
    this.selectionChange.emit(supplier.id);
  }
}
