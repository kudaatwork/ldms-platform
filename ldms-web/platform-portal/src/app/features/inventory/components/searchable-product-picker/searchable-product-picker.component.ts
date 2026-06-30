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
import { ProductRow } from '../../models/inventory.model';
import { filterProductsForSearch, productPickerLabel } from '../../utils/product-search.util';

const MAX_VISIBLE_RESULTS = 50;

@Component({
  selector: 'app-searchable-product-picker',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatAutocompleteModule],
  templateUrl: './searchable-product-picker.component.html',
  styleUrl: './searchable-product-picker.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SearchableProductPickerComponent),
      multi: true,
    },
  ],
})
export class SearchableProductPickerComponent implements ControlValueAccessor {
  private static nextId = 0;
  private readonly instanceId = ++SearchableProductPickerComponent.nextId;

  @ViewChild(MatAutocompleteTrigger) private autocompleteTrigger?: MatAutocompleteTrigger;

  @Input() products: ProductRow[] = [];
  @Input() label = 'Product';
  @Input() placeholder = 'Select product';
  @Input() required = false;
  @Input() invalid = false;
  @Input() disabled = false;
  @Input() searchPlaceholder = 'Type or scan barcode, name, or code…';
  @Input() showCodeInLabel = true;
  @Input() inputId = '';
  @Input() ariaLabel = 'Product';
  /** When set, shows supplier available stock in autocomplete options (product id → quantity). */
  @Input() stockByProductId: Readonly<Record<number, number>> | null = null;
  @Input() stockUnitByProductId: Readonly<Record<number, string>> | null = null;

  @Output() selectionChange = new EventEmitter<number | null>();

  readonly maxVisibleResults = MAX_VISIBLE_RESULTS;

  searchText = '';
  value: number | null = null;

  private onChange: (value: number | null) => void = () => {};
  private onTouched: () => void = () => {};

  get inputElementId(): string {
    return this.inputId || `product-picker-input-${this.instanceId}`;
  }

  get searchQuery(): string {
    return this.asSearchString(this.searchText);
  }

  get filteredProducts(): ProductRow[] {
    return filterProductsForSearch(this.products, this.searchQuery).slice(0, MAX_VISIBLE_RESULTS);
  }

  get totalFilteredCount(): number {
    return filterProductsForSearch(this.products, this.searchQuery).length;
  }

  get hasMoreResults(): boolean {
    return this.totalFilteredCount > MAX_VISIBLE_RESULTS;
  }

  productLabel(p: ProductRow): string {
    if (!this.showCodeInLabel) {
      return p.name?.trim() || `#${p.id}`;
    }
    return productPickerLabel(p);
  }

  productOptionValue(p: ProductRow): string {
    return this.productLabel(p);
  }

  productStockLabel(p: ProductRow): string {
    if (!this.stockByProductId) {
      return '';
    }
    const qty = this.stockByProductId[p.id];
    if (qty == null) {
      return 'No stock data';
    }
    const uom = this.stockUnitByProductId?.[p.id]?.trim() || p.unitOfMeasure || 'units';
    if (qty <= 0) {
      return `Out of stock (0 ${uom})`;
    }
    return `${qty} ${uom} available at supplier`;
  }

  productMeta(p: ProductRow): string {
    const stock = this.productStockLabel(p);
    if (stock) {
      return stock;
    }
    return p.categoryName?.trim() ?? '';
  }

  isOutOfStock(p: ProductRow): boolean {
    if (!this.stockByProductId) {
      return false;
    }
    const qty = this.stockByProductId[p.id];
    return qty != null && qty <= 0;
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
    if (this.coerceNumericInputToProductLabel()) {
      return;
    }
    const selectedLabel = this.labelForId(this.value);
    if (this.value != null && this.searchQuery !== selectedLabel) {
      this.value = null;
      this.onChange(null);
      this.selectionChange.emit(null);
    }
  }

  onProductPicked(event: MatAutocompleteSelectedEvent): void {
    const product = this.productFromOptionValue(event.option.value);
    if (!product) {
      return;
    }
    this.applyProductSelection(product);
    this.autocompleteTrigger?.closePanel();
  }

  onBlur(): void {
    this.onTouched();
    this.normaliseSearchText();
    this.restoreLabelWhenInputMatchesSelectedId();
    this.coerceNumericInputToProductLabel();
    const selectedLabel = this.labelForId(this.value);
    if (this.value != null && this.searchQuery !== selectedLabel) {
      this.searchText = selectedLabel;
    }
  }

  onSearchKeydown(event: Event): void {
    if (!(event instanceof KeyboardEvent) || event.key !== 'Enter') {
      return;
    }
    const matches = filterProductsForSearch(this.products, this.searchQuery);
    if (matches.length !== 1) {
      return;
    }
    event.preventDefault();
    this.applyProductSelection(matches[0]);
    this.autocompleteTrigger?.closePanel();
  }

  onSelectChange(id: number | null): void {
    this.value = id;
    this.onChange(id);
    this.onTouched();
    this.selectionChange.emit(id);
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

  private labelForId(id: number | null): string {
    if (id == null) {
      return '';
    }
    const product = this.products.find((p) => p.id === id);
    return product ? this.productLabel(product) : '';
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

  private productFromOptionValue(optionValue: unknown): ProductRow | null {
    const raw = this.asSearchString(optionValue);
    if (!raw) {
      return null;
    }
    if (/^\d+$/.test(raw)) {
      const id = Number(raw);
      return this.products.find((p) => p.id === id) ?? null;
    }
    const byLabel = this.products.filter((p) => this.productLabel(p) === raw);
    if (byLabel.length === 1) {
      return byLabel[0];
    }
    if (byLabel.length > 1 && this.value != null) {
      return byLabel.find((p) => p.id === this.value) ?? byLabel[0];
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

  private coerceNumericInputToProductLabel(): boolean {
    if (!/^\d+$/.test(this.searchQuery)) {
      return false;
    }
    const product = this.products.find((p) => p.id === Number(this.searchQuery));
    if (!product) {
      return false;
    }
    this.applyProductSelection(product);
    return true;
  }

  private applyProductSelection(product: ProductRow): void {
    this.onSelectChange(product.id);
    this.searchText = this.productLabel(product);
  }
}
