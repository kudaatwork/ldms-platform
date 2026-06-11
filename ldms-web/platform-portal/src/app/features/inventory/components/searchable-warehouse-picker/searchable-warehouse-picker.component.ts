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
import { WarehouseRow } from '../../models/inventory.model';
import {
  filterWarehousesForSearch,
  warehousePickerLabel,
  warehouseSearchText,
} from '../../utils/warehouse-search.util';

@Component({
  selector: 'app-searchable-warehouse-picker',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatAutocompleteModule],
  templateUrl: './searchable-warehouse-picker.component.html',
  styleUrl: './searchable-warehouse-picker.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SearchableWarehousePickerComponent),
      multi: true,
    },
  ],
})
export class SearchableWarehousePickerComponent implements ControlValueAccessor {
  private static nextId = 0;
  private readonly instanceId = ++SearchableWarehousePickerComponent.nextId;

  @ViewChild(MatAutocompleteTrigger) private autocompleteTrigger?: MatAutocompleteTrigger;

  @Input() warehouses: WarehouseRow[] = [];
  @Input() label = 'Warehouse';
  @Input() required = false;
  @Input() invalid = false;
  @Input() disabled = false;
  @Input() searchPlaceholder = 'Type to search by name, address, or type…';
  @Input() inputId = '';
  @Input() ariaLabel = 'Warehouse';
  /** When set, matching options are shown but cannot be selected (e.g. already stocked). */
  @Input() optionDisabled: ((warehouse: WarehouseRow) => boolean) | null = null;
  @Input() disabledOptionSuffix = 'already stocked';
  @Input() emptyHint = '';

  @Output() selectionChange = new EventEmitter<number | null>();

  searchText = '';
  value: number | null = null;

  private onChange: (value: number | null) => void = () => {};
  private onTouched: () => void = () => {};

  get inputElementId(): string {
    return this.inputId || `warehouse-picker-input-${this.instanceId}`;
  }

  get filteredWarehouses(): WarehouseRow[] {
    const matched = filterWarehousesForSearch(this.warehouses, this.searchText);
    if (!this.optionDisabled) {
      return matched;
    }
    return [...matched].sort((a, b) => {
      const aDisabled = this.isOptionDisabled(a) ? 1 : 0;
      const bDisabled = this.isOptionDisabled(b) ? 1 : 0;
      return aDisabled - bDisabled;
    });
  }

  warehouseLabel(w: WarehouseRow): string {
    return warehousePickerLabel(w);
  }

  warehouseMeta(w: WarehouseRow): string {
    const parts = [w.addressLabel?.trim(), w.warehouseType?.trim(), w.description?.trim()].filter(Boolean);
    return parts.join(' · ');
  }

  warehouseSearchBlob(w: WarehouseRow): string {
    return warehouseSearchText(w);
  }

  isOptionDisabled(w: WarehouseRow): boolean {
    return this.optionDisabled?.(w) ?? false;
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
    setTimeout(() => this.autocompleteTrigger?.openPanel());
  }

  onWarehousePicked(event: MatAutocompleteSelectedEvent): void {
    const warehouse = event.option.value as WarehouseRow;
    if (!warehouse || this.isOptionDisabled(warehouse)) {
      return;
    }
    this.onSelectChange(warehouse.id);
    this.searchText = this.warehouseLabel(warehouse);
  }

  onBlur(): void {
    this.onTouched();
    const selectedLabel = this.labelForId(this.value);
    if (this.value != null && this.searchText.trim() !== selectedLabel) {
      this.searchText = selectedLabel;
    }
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
    const warehouse = this.warehouses.find((w) => w.id === id);
    return warehouse ? this.warehouseLabel(warehouse) : '';
  }
}
