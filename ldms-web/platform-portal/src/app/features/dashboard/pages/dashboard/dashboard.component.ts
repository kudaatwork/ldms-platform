import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { OrganizationClassification } from '../../../../core/models/auth.model';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import {
  KpiCard,
  PLATFORM_KPI_CONFIG,
  SUPPLIER_SHIPMENT_MOCKS,
  SupplierShipmentCard,
  SupplierShipmentStatus,
} from '../../data/platform-mock-data';

type ShipmentFilter = 'ALL' | SupplierShipmentStatus;

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DashboardComponent implements OnInit {
  cards: KpiCard[] = [];
  classification: OrganizationClassification | '' = '';
  classificationLabel = '';

  readonly supplierShipments = SUPPLIER_SHIPMENT_MOCKS;

  shipmentFilter: ShipmentFilter = 'ALL';
  shipmentSearch = '';
  selectedShipmentId: string | null = SUPPLIER_SHIPMENT_MOCKS[0]?.id ?? null;

  constructor(
    private readonly authState: AuthStateService,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    const user = this.authState.currentUser;
    this.classification = user?.orgClassification ?? '';
    this.classificationLabel = this.formatClassification(this.classification);
    this.cards = user ? PLATFORM_KPI_CONFIG[user.orgClassification] : [];
    if (this.isSupplier) {
      this.selectedShipmentId = this.filteredShipments[0]?.id ?? null;
    }
  }

  get isSupplier(): boolean {
    return this.classification === 'SUPPLIER';
  }

  get shipmentCounts(): Record<ShipmentFilter, number> {
    const all = this.supplierShipments.length;
    const by = (s: SupplierShipmentStatus) => this.supplierShipments.filter((x) => x.status === s).length;
    return {
      ALL: all,
      PREPARED: by('PREPARED'),
      IN_TRANSIT: by('IN_TRANSIT'),
      COMPLETED: by('COMPLETED'),
      FAILED: by('FAILED'),
    };
  }

  get filteredShipments(): SupplierShipmentCard[] {
    const q = this.shipmentSearch.trim().toLowerCase();
    return this.supplierShipments.filter((s) => {
      if (this.shipmentFilter !== 'ALL' && s.status !== this.shipmentFilter) {
        return false;
      }
      if (!q) {
        return true;
      }
      return (
        s.shipmentNo.toLowerCase().includes(q) ||
        s.category.toLowerCase().includes(q) ||
        s.driver.toLowerCase().includes(q) ||
        s.departureLabel.toLowerCase().includes(q) ||
        s.arrivalLabel.toLowerCase().includes(q)
      );
    });
  }

  get selectedShipment(): SupplierShipmentCard | undefined {
    return this.supplierShipments.find((s) => s.id === this.selectedShipmentId);
  }

  setFilter(f: ShipmentFilter): void {
    this.shipmentFilter = f;
    const list = this.filteredShipments;
    if (!list.some((s) => s.id === this.selectedShipmentId)) {
      this.selectedShipmentId = list[0]?.id ?? null;
    }
    this.cdr.markForCheck();
  }

  onSearchInput(): void {
    const list = this.filteredShipments;
    if (!list.some((s) => s.id === this.selectedShipmentId)) {
      this.selectedShipmentId = list[0]?.id ?? null;
    }
    this.cdr.markForCheck();
  }

  selectShipment(s: SupplierShipmentCard): void {
    this.selectedShipmentId = s.id;
    this.cdr.markForCheck();
  }

  goNewShipment(): void {
    void this.router.navigate(['/shipments']);
  }

  statusClass(status: SupplierShipmentStatus): string {
    switch (status) {
      case 'PREPARED':
        return 'dash-ship-badge--prepared';
      case 'IN_TRANSIT':
        return 'dash-ship-badge--transit';
      case 'COMPLETED':
        return 'dash-ship-badge--done';
      case 'FAILED':
        return 'dash-ship-badge--fail';
      default:
        return '';
    }
  }

  statusLabel(status: SupplierShipmentStatus): string {
    switch (status) {
      case 'PREPARED':
        return 'Prepared';
      case 'IN_TRANSIT':
        return 'In transit';
      case 'COMPLETED':
        return 'Completed';
      case 'FAILED':
        return 'Failed';
      default:
        return status;
    }
  }

  trackShipment(_i: number, s: SupplierShipmentCard): string {
    return s.id;
  }

  private formatClassification(raw: string): string {
    if (!raw) {
      return '';
    }
    return raw
      .split('_')
      .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
      .join(' ');
  }
}
