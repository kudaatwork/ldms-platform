import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DocumentsStagingComponent } from './pages/documents-staging/documents-staging.component';

const routes: Routes = [
  {
    path: '',
    component: DocumentsStagingComponent,
    data: { title: 'Documents', breadcrumb: 'Documents' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class DocumentsRoutingModule {}
