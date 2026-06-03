import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { DocumentsRoutingModule } from './documents-routing.module';
import { DocumentsStagingComponent } from './pages/documents-staging/documents-staging.component';

@NgModule({
  declarations: [DocumentsStagingComponent],
  imports: [SharedModule, DocumentsRoutingModule],
})
export class DocumentsModule {}
