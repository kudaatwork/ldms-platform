import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { UserAddressCascadeFieldsComponent } from '../users/components/user-address-cascade-fields/user-address-cascade-fields.component';
import { AddWarehouseDialogComponent } from './components/add-warehouse-dialog/add-warehouse-dialog.component';

/** Dialog-only exports for organisation management and other feature modules (no routing). */
@NgModule({
  declarations: [AddWarehouseDialogComponent],
  imports: [SharedModule, UserAddressCascadeFieldsComponent],
  exports: [AddWarehouseDialogComponent],
})
export class InventoryDialogsModule {}
