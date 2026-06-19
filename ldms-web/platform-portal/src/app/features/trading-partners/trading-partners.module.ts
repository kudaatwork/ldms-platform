import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { TradingPartnersRoutingModule } from './trading-partners-routing.module';
import { TradingPartnersWorkspaceComponent } from './pages/trading-partners-workspace/trading-partners-workspace.component';
import { TradingPartnerDialogComponent } from './components/trading-partner-dialog/trading-partner-dialog.component';

@NgModule({
  declarations: [TradingPartnersWorkspaceComponent, TradingPartnerDialogComponent],
  imports: [SharedModule, TradingPartnersRoutingModule],
})
export class TradingPartnersModule {}
