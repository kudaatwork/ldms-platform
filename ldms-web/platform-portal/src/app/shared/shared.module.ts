import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatBadgeModule } from '@angular/material/badge';
import { MatBottomSheetModule } from '@angular/material/bottom-sheet';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSliderModule } from '@angular/material/slider';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSortModule } from '@angular/material/sort';
import { MatStepperModule } from '@angular/material/stepper';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTreeModule } from '@angular/material/tree';

import { LxTablePaginatorDirective } from './directives/lx-table-paginator.directive';
import { LxPageHeroActionsDirective } from './directives/lx-page-hero-actions.directive';
import { LxWorkspaceHeroActionsDirective } from './directives/lx-workspace-hero-actions.directive';
import { DeleteConfirmDialogComponent } from './components/delete-confirm-dialog/delete-confirm-dialog.component';
import { LdmsPasswordRequirementsComponent } from './components/ldms-password-requirements/ldms-password-requirements.component';
import { LxInlineBusyComponent } from './components/lx-inline-busy/lx-inline-busy.component';
import { LxTableLoadingComponent } from './components/lx-table-loading/lx-table-loading.component';
import { LxPageHeroComponent } from './components/lx-page-hero/lx-page-hero.component';
import { LxWorkspaceHeroComponent } from './components/lx-workspace-hero/lx-workspace-hero.component';
import { LxWorkspaceHeroStatComponent } from './components/lx-workspace-hero-stat/lx-workspace-hero-stat.component';
import { PhoneVerificationDialogComponent } from './components/phone-verification-dialog/phone-verification-dialog.component';
import { LxOrganizationMetadataPanelComponent } from './components/lx-organization-metadata-panel/lx-organization-metadata-panel.component';

const MATERIAL_MODULES = [
  MatAutocompleteModule,
  MatBadgeModule,
  MatBottomSheetModule,
  MatButtonModule,
  MatButtonToggleModule,
  MatCardModule,
  MatCheckboxModule,
  MatChipsModule,
  MatDatepickerModule,
  MatDialogModule,
  MatDividerModule,
  MatExpansionModule,
  MatFormFieldModule,
  MatGridListModule,
  MatIconModule,
  MatInputModule,
  MatListModule,
  MatMenuModule,
  MatNativeDateModule,
  MatPaginatorModule,
  MatProgressBarModule,
  MatProgressSpinnerModule,
  MatRadioModule,
  MatSelectModule,
  MatSidenavModule,
  MatSlideToggleModule,
  MatSliderModule,
  MatSnackBarModule,
  MatSortModule,
  MatStepperModule,
  MatTableModule,
  MatTabsModule,
  MatToolbarModule,
  MatTooltipModule,
  MatTreeModule,
];

@NgModule({
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    LxTablePaginatorDirective,
    ...MATERIAL_MODULES,
  ],
  declarations: [
    DeleteConfirmDialogComponent,
    LdmsPasswordRequirementsComponent,
    LxInlineBusyComponent,
    LxTableLoadingComponent,
    LxPageHeroComponent,
    LxPageHeroActionsDirective,
    LxWorkspaceHeroActionsDirective,
    LxOrganizationMetadataPanelComponent,
    LxWorkspaceHeroComponent,
    LxWorkspaceHeroStatComponent,
    PhoneVerificationDialogComponent,
  ],
  exports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    LxTablePaginatorDirective,
    LxPageHeroActionsDirective,
    LxWorkspaceHeroActionsDirective,
    DeleteConfirmDialogComponent,
    LdmsPasswordRequirementsComponent,
    LxInlineBusyComponent,
    LxTableLoadingComponent,
    LxPageHeroComponent,
    LxOrganizationMetadataPanelComponent,
    LxWorkspaceHeroComponent,
    LxWorkspaceHeroStatComponent,
    ...MATERIAL_MODULES,
  ],
})
export class SharedModule {}
