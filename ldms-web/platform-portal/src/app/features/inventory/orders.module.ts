import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { OrdersRoutingModule } from './orders-routing.module';
import { CreateRequisitionDialogComponent } from './components/create-requisition-dialog/create-requisition-dialog.component';
import { ReceiveGoodsDialogComponent } from './components/receive-goods-dialog/receive-goods-dialog.component';
import { OrdersWorkspaceComponent } from './pages/orders-workspace/orders-workspace.component';
import { SearchableWarehousePickerComponent } from './components/searchable-warehouse-picker/searchable-warehouse-picker.component';
import { InventoryDialogsModule } from './inventory-dialogs.module';
import { InventoryModule } from './inventory.module';

/** Customer inventory & procurement workspace (/my-orders). */
@NgModule({
  declarations: [OrdersWorkspaceComponent, ReceiveGoodsDialogComponent],
  imports: [
    SharedModule,
    OrdersRoutingModule,
    InventoryDialogsModule,
    InventoryModule,
    CreateRequisitionDialogComponent,
    SearchableWarehousePickerComponent,
  ],
})
export class OrdersModule {}
