import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { InventoryRoutingModule } from './inventory-routing.module';
import { InventoryWorkspaceComponent } from './pages/inventory-workspace/inventory-workspace.component';
import { CategoryDialogComponent } from './components/category-dialog/category-dialog.component';
import type { CategoryDialogData } from './components/category-dialog/category-dialog.component';
import { SubcategoryDialogComponent } from './components/subcategory-dialog/subcategory-dialog.component';
import type { SubcategoryDialogData } from './components/subcategory-dialog/subcategory-dialog.component';
import { AddProductDialogComponent } from './components/add-product-dialog/add-product-dialog.component';
import { AddWarehouseDialogComponent } from './components/add-warehouse-dialog/add-warehouse-dialog.component';
import { CreateTransferDialogComponent } from './components/create-transfer-dialog/create-transfer-dialog.component';
import { InitialStockDialogComponent } from './components/initial-stock-dialog/initial-stock-dialog.component';
import { ReplenishStockDialogComponent } from './components/replenish-stock-dialog/replenish-stock-dialog.component';
import { InventoryDetailDialogComponent } from './components/inventory-detail-dialog/inventory-detail-dialog.component';
import { ViewTransferDialogComponent } from './components/view-transfer-dialog/view-transfer-dialog.component';
import { SubmitSupplierQuoteDialogComponent } from './components/submit-supplier-quote-dialog/submit-supplier-quote-dialog.component';
import { UserAddressCascadeFieldsComponent } from '../users/components/user-address-cascade-fields/user-address-cascade-fields.component';
import { SearchableProductPickerComponent } from './components/searchable-product-picker/searchable-product-picker.component';
import { SearchableWarehousePickerComponent } from './components/searchable-warehouse-picker/searchable-warehouse-picker.component';

/** SUPPLIER inventory module: products, warehouses, stock levels, and transfers. */
@NgModule({
  declarations: [
    InventoryWorkspaceComponent,
    CategoryDialogComponent,
    SubcategoryDialogComponent,
    AddProductDialogComponent,
    AddWarehouseDialogComponent,
    CreateTransferDialogComponent,
    InitialStockDialogComponent,
    ReplenishStockDialogComponent,
    InventoryDetailDialogComponent,
    ViewTransferDialogComponent,
    SubmitSupplierQuoteDialogComponent,
  ],
  imports: [
    SharedModule,
    InventoryRoutingModule,
    UserAddressCascadeFieldsComponent,
    SearchableProductPickerComponent,
    SearchableWarehousePickerComponent,
  ],
})
export class InventoryModule {}
