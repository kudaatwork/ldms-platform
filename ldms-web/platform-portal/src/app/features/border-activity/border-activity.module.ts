import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { BorderActivityRoutingModule } from './border-activity-routing.module';
import { BorderActivityWorkspaceComponent } from './pages/border-activity-workspace/border-activity-workspace.component';

@NgModule({
  declarations: [BorderActivityWorkspaceComponent],
  imports: [SharedModule, BorderActivityRoutingModule],
})
export class BorderActivityModule {}
