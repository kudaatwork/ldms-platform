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

  @Output() selectionChange = new EventEmitter<number | null>();

  readonly maxVisibleResults = MAX_VISIBLE_RESULTS;

  searchText = '';
  value: number | null = null;

  private onChange: (value: number | null) => void = () => {};
  private onTouched: () => void = () => {};

  get inputElementId(): string {
    return this.inputId || `product-picker-input-${this.instanceId}`;
  }

  get filteredProducts(): ProductRow[] {
    return filterProductsForSearch(this.products, this.searchText).slice(0, MAX_VISIBLE_RESULTS);
  }

  get totalFilteredCount(): number {
    return filterProductsForSearch(this.products, this.searchText).length;
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

  onInputFocus(): void {
    setTimeout(() => this.autocompleteTrigger?.openPanel());
  }

  onSearchInput(): void {
    const selectedLabel = this.labelForId(this.value);
    if (this.value != null && this.searchText.trim() !== selectedLabel) {
      this.value = null;
      this.onChange(null);
      this.selectionChange.emit(null);
    }
  }

  onProductPicked(event: MatAutocompleteSelectedEvent): void {
    const product = event.option.value as ProductRow;
    this.onSelectChange(product.id);
    this.searchText = this.productLabel(product);
  }

  onBlur(): void {
    this.onTouched();
    const selectedLabel = this.labelForId(this.value);
    if (this.value != null && this.searchText.trim() !== selectedLabel) {
      this.searchText = selectedLabel;
    }
  }

  onSearchKeydown(event: Event): void {
    if (!(event instanceof KeyboardEvent) || event.key !== 'Enter') {
      return;
    }
    const matches = filterProductsForSearch(this.products, this.searchText);
    if (matches.length !== 1) {
      return;
    }
    event.preventDefault();
    const product = matches[0];
    this.onSelectChange(product.id);
    this.searchText = this.productLabel(product);
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
}
