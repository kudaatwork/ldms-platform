import { PageEvent } from '@angular/material/paginator';
import {
  DEFAULT_TABLE_PAGE_SIZE,
  DEFAULT_TABLE_PAGE_SIZE_OPTIONS,
} from '../../../shared/constants/table-pagination';

export const FLEET_TABLE_PAGE_SIZE_OPTIONS = DEFAULT_TABLE_PAGE_SIZE_OPTIONS;
export const FLEET_CARD_PAGE_SIZE_OPTIONS = [12, 24, 48];
export const FLEET_CARD_DEFAULT_PAGE_SIZE = 12;

/** Client-side page state for fleet workspace lists and card grids. */
export class FleetTablePage {
  index = 0;
  size: number;

  constructor(initialSize = DEFAULT_TABLE_PAGE_SIZE) {
    this.size = initialSize;
  }

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

export function fleetPageSummary(page: FleetTablePage, total: number): string {
  if (total <= 0) {
    return 'No records';
  }
  const start = page.index * page.size + 1;
  const end = Math.min((page.index + 1) * page.size, total);
  return `${start}–${end} of ${total}`;
}
