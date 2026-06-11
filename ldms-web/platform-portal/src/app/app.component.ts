import { Component } from '@angular/core';
import { NetworkConnectivityService } from './core/services/network-connectivity.service';
import { OfflinePageComponent } from './shared/components/offline-page/offline-page.component';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  standalone: false,
  styleUrl: './app.component.scss',
})
export class AppComponent {
  constructor(readonly connectivity: NetworkConnectivityService) {}
}
