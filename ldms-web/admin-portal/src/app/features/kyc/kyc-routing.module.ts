import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { KycApplicationsComponent } from './pages/kyc-applications/kyc-applications.component';
import { KycDocumentsComponent } from './pages/kyc-documents/kyc-documents.component';

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'applications' },
  {
    path: 'applications',
    component: KycApplicationsComponent,
    data: { title: 'KYC Applications', breadcrumb: 'Applications' },
  },
  {
    path: 'documents',
    component: KycDocumentsComponent,
    data: { title: 'KYC Documents', breadcrumb: 'Documents' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class KycRoutingModule {}
