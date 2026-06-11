import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { OrdersRoutingModule } from './orders-routing.module';
import { CreateRequisitionDialogComponent } from './components/create-requisition-dialog/create-requisition-dialog.component';
import { ReceiveGoodsDialogComponent } from './components/receive-goods-dialog/receive-goods-dialog.component';
import { OrdersWorkspaceComponent } from './pages/orders-workspace/orders-workspace.component';
import { SearchableProductPickerComponent } from './components/searchable-product-picker/searchable-product-picker.component';
import { SearchableWarehousePickerComponent } from './components/searchable-warehouse-picker/searchable-warehouse-picker.component';

/** Thin module for the CUSTOMER "My Orders" route (/my-orders). */
@NgModule({
  declarations: [OrdersWorkspaceComponent, CreateRequisitionDialogComponent, ReceiveGoodsDialogComponent],
  imports: [SharedModule, OrdersRoutingModule, SearchableProductPickerComponent, SearchableWarehousePickerComponent],
})
export class OrdersModule {}
