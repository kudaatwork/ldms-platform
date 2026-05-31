import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { AdminSupportTicket, HelpSupportAdminService } from '../../services/help-support-admin.service';

type StatusFilter = 'ALL' | 'OPEN' | 'IN_PROGRESS' | 'WAITING_ON_CUSTOMER' | 'RESOLVED' | 'CLOSED';

@Component({
  selector: 'app-help-support-admin-page',
  templateUrl: './help-support-admin-page.component.html',
  styleUrl: './help-support-admin-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class HelpSupportAdminPageComponent implements OnInit {
  loading = true;
  error = '';
  tickets: AdminSupportTicket[] = [];
  selected: AdminSupportTicket | null = null;

  readonly search = signal('');
  readonly statusFilter = signal<StatusFilter>('ALL');

  readonly statusFilters: { id: StatusFilter; label: string }[] = [
    { id: 'ALL', label: 'All' },
    { id: 'OPEN', label: 'Open' },
    { id: 'IN_PROGRESS', label: 'In progress' },
    { id: 'WAITING_ON_CUSTOMER', label: 'Waiting' },
    { id: 'RESOLVED', label: 'Resolved' },
    { id: 'CLOSED', label: 'Closed' },
  ];

  constructor(
    private readonly title: Title,
    private readonly helpApi: HelpSupportAdminService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.title.setTitle('Help & Support | LX Admin');
    this.reload();
  }

  get openTicketCount(): number {
    return this.tickets.filter((t) => t.status === 'OPEN').length;
  }

  get inProgressCount(): number {
    return this.tickets.filter((t) => t.status === 'IN_PROGRESS').length;
  }

  get waitingCount(): number {
    return this.tickets.filter((t) => t.status === 'WAITING_ON_CUSTOMER').length;
  }

  get filteredTickets(): AdminSupportTicket[] {
    const q = this.search().trim().toLowerCase();
    const status = this.statusFilter();
    return this.tickets.filter((t) => {
      if (status !== 'ALL' && t.status !== status) {
        return false;
      }
      if (!q) {
        return true;
      }
      return (
        t.ticketNumber.toLowerCase().includes(q) ||
        t.subject.toLowerCase().includes(q) ||
        t.requesterUsername.toLowerCase().includes(q) ||
        t.requesterEmail.toLowerCase().includes(q) ||
        t.category.toLowerCase().includes(q)
      );
    });
  }

  reload(): void {
    this.loading = true;
    this.error = '';
    this.helpApi.fetchAllTickets().subscribe({
      next: (tickets) => {
        this.tickets = tickets;
        this.loading = false;
        if (tickets.length && !this.selected) {
          this.selected = tickets[0];
        } else if (this.selected && !tickets.some((t) => t.ticketNumber === this.selected?.ticketNumber)) {
          this.selected = tickets[0] ?? null;
        }
        this.cdr.markForCheck();
      },
      error: (err: Error) => {
        this.error = err.message;
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  selectTicket(ticket: AdminSupportTicket): void {
    this.selected = ticket;
    this.cdr.markForCheck();
  }

  setStatusFilter(filter: StatusFilter): void {
    this.statusFilter.set(filter);
    const rows = this.filteredTickets;
    if (rows.length && !rows.some((t) => t.ticketNumber === this.selected?.ticketNumber)) {
      this.selected = rows[0];
      this.cdr.markForCheck();
    }
  }

  goSystemHealth(): void {
    void this.router.navigate(['/system/health']);
  }

  statusLabel(status: string): string {
    return status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
  }

  categoryIcon(category: string): string {
    switch (category) {
      case 'TECHNICAL':
        return 'build';
      case 'BILLING':
        return 'receipt_long';
      case 'ACCESS':
        return 'vpn_key';
      case 'SECURITY':
        return 'shield';
      case 'OPERATIONS':
        return 'local_shipping';
      default:
        return 'support_agent';
    }
  }

  priorityTone(priority: string): string {
    switch (priority) {
      case 'URGENT':
        return 'urgent';
      case 'HIGH':
        return 'high';
      case 'LOW':
        return 'low';
      default:
        return 'normal';
    }
  }

  trackTicket(_: number, t: AdminSupportTicket): string {
    return t.ticketNumber;
  }
}
