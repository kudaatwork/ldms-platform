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

  constructor(private readonly authState: AuthStateService) {}

  ngOnInit(): void {
    const user = this.authState.currentUser;
    this.classification = user?.orgClassification ?? '';
    this.cards = user ? PLATFORM_KPI_CONFIG[user.orgClassification] : [];
  }
}
