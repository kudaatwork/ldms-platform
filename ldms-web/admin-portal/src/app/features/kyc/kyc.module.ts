import { NgModule } from '@angular/core';
import { KycRoutingModule } from './kyc-routing.module';
import { KycApplicationsComponent } from './pages/kyc-applications/kyc-applications.component';
import { KycApplicationDeleteDialogComponent } from './pages/kyc-application-delete-dialog/kyc-application-delete-dialog.component';
import { KycApplicationDetailDialogComponent } from './pages/kyc-application-detail-dialog/kyc-application-detail-dialog.component';
import { KycApplicationEditDialogComponent } from './pages/kyc-application-edit-dialog/kyc-application-edit-dialog.component';
import { KycDocumentsComponent } from './pages/kyc-documents/kyc-documents.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [KycApplicationsComponent, KycDocumentsComponent],
  imports: [
    SharedModule,
    KycRoutingModule,
    KycApplicationDeleteDialogComponent,
    KycApplicationDetailDialogComponent,
    KycApplicationEditDialogComponent,
  ],
})
export class KycModule {}
