import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { RoadsideSupportRoutingModule } from './roadside-support-routing.module';
import { RoadsideWorkspaceComponent } from './pages/roadside-workspace/roadside-workspace.component';

@NgModule({
  declarations: [RoadsideWorkspaceComponent],
  imports: [SharedModule, RoadsideSupportRoutingModule],
})
export class RoadsideSupportModule {}
