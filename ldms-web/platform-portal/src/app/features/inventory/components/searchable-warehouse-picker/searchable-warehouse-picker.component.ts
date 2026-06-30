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
  warehouseOptionMeta,
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
  @Input() searchPlaceholder = 'Search warehouse by name, branch, or address…';
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

  /** Normalised query — Material may briefly write a numeric option value into the input. */
  get searchQuery(): string {
    return this.asSearchString(this.searchText);
  }

  get filteredWarehouses(): WarehouseRow[] {
    const matched = filterWarehousesForSearch(this.warehouses, this.searchQuery);
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

  /** Option value written by mat-autocomplete — use the display label, not the numeric id. */
  warehouseOptionValue(w: WarehouseRow): string {
    return this.warehouseLabel(w);
  }

  warehouseSearchBlob(w: WarehouseRow): string {
    return warehouseSearchText(w);
  }

  warehouseMeta(w: WarehouseRow): string {
    return warehouseOptionMeta(w);
  }

  isOptionDisabled(w: WarehouseRow): boolean {
    return this.optionDisabled?.(w) ?? false;
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
    if (this.coerceNumericInputToWarehouseLabel()) {
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

  onWarehousePicked(event: MatAutocompleteSelectedEvent): void {
    const warehouse = this.warehouseFromOptionValue(event.option.value);
    if (!warehouse || this.isOptionDisabled(warehouse)) {
      return;
    }
    this.applyWarehouseSelection(warehouse);
    this.autocompleteTrigger?.closePanel();
  }

  onBlur(): void {
    this.onTouched();
    this.normaliseSearchText();
    this.restoreLabelWhenInputMatchesSelectedId();
    this.coerceNumericInputToWarehouseLabel();
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
    if (raw == null) {
      return '';
    }
    if (typeof raw === 'object') {
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
    const warehouse = this.warehouses.find((w) => w.id === id);
    return warehouse ? this.warehouseLabel(warehouse) : '';
  }

  private warehouseFromOptionValue(optionValue: unknown): WarehouseRow | null {
    const raw = this.asSearchString(optionValue);
    if (!raw) {
      return null;
    }
    if (/^\d+$/.test(raw)) {
      const id = Number(raw);
      return this.warehouses.find((w) => w.id === id) ?? null;
    }
    const byLabel = this.warehouses.filter((w) => this.warehouseLabel(w) === raw);
    if (byLabel.length === 1) {
      return byLabel[0];
    }
    if (byLabel.length > 1 && this.value != null) {
      return byLabel.find((w) => w.id === this.value) ?? byLabel[0];
    }
    return byLabel[0] ?? null;
  }

  /** Material autocomplete may write the selected warehouse id into the text input. */
  private restoreLabelWhenInputMatchesSelectedId(): boolean {
    if (this.value == null) {
      return false;
    }
    if (this.searchQuery === String(this.value)) {
      this.searchText = this.labelForId(this.value);
      return true;
    }
    return false;
  }

  private coerceNumericInputToWarehouseLabel(): boolean {
    if (!/^\d+$/.test(this.searchQuery)) {
      return false;
    }
    const id = Number(this.searchQuery);
    const warehouse = this.warehouses.find((w) => w.id === id);
    if (!warehouse) {
      return false;
    }
    this.applyWarehouseSelection(warehouse);
    return true;
  }

  private applyWarehouseSelection(warehouse: WarehouseRow): void {
    this.value = warehouse.id;
    this.searchText = this.warehouseLabel(warehouse);
    this.onChange(warehouse.id);
    this.selectionChange.emit(warehouse.id);
  }
}
