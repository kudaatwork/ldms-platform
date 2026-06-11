import { PageEvent } from '@angular/material/paginator';
import {
  DEFAULT_TABLE_PAGE_SIZE,
  DEFAULT_TABLE_PAGE_SIZE_OPTIONS,
} from '../../../shared/constants/table-pagination';

export const INVENTORY_PAGE_SIZE_OPTIONS = DEFAULT_TABLE_PAGE_SIZE_OPTIONS;

/** Client-side page state for inventory workspace tables. */
export class InventoryTablePage {
  index = 0;
  size = DEFAULT_TABLE_PAGE_SIZE;

  slice<T>(rows: readonly T[]): T[] {
    const start = this.index * this.size;
    return rows.slice(start, start + this.size);
  }

  onPage(event: PageEvent): void {
    this.index = event.pageIndex;
    this.size = event.pageSize;
  }

  reset(): void {
    this.index = 0;
  }

  clamp(total: number): void {
    const maxIndex = Math.max(0, Math.ceil(total / this.size) - 1);
    if (this.index > maxIndex) {
      this.index = maxIndex;
    }
  }
}

export function inventoryPageSummary(page: InventoryTablePage, total: number): string {
  if (total <= 0) {
    return 'No records';
  }
  const start = page.index * page.size + 1;
  const end = Math.min((page.index + 1) * page.size, total);
  return `${start}–${end} of ${total}`;
}
