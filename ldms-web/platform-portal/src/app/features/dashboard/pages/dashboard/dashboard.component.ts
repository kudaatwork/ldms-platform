import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { KpiCard, PLATFORM_KPI_CONFIG } from '../../data/platform-mock-data';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DashboardComponent implements OnInit {
  cards: KpiCard[] = [];
  classification = '';
  classificationLabel = '';

  constructor(private readonly authState: AuthStateService) {}

  ngOnInit(): void {
    const user = this.authState.currentUser;
    this.classification = user?.orgClassification ?? '';
    this.classificationLabel = this.formatClassification(this.classification);
    this.cards = user ? PLATFORM_KPI_CONFIG[user.orgClassification] : [];
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
