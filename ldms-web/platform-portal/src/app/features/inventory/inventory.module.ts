import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { InventoryRoutingModule } from './inventory-routing.module';
import { InventoryWorkspaceComponent } from './pages/inventory-workspace/inventory-workspace.component';
import { InventoryIntegrationApiPageComponent } from './pages/inventory-integration-api-page/inventory-integration-api-page.component';
import { InventoryIntegrationSubnavComponent } from './components/inventory-integration-subnav/inventory-integration-subnav.component';
import { InventoryIntegrationSetupPanelComponent } from './components/inventory-integration-setup-panel/inventory-integration-setup-panel.component';
import { LogisticsRouteJourneyComponent } from './components/logistics-route-journey/logistics-route-journey.component';
import { EditTransferDialogComponent } from './components/edit-transfer-dialog/edit-transfer-dialog.component';
import { CategoryDialogComponent } from './components/category-dialog/category-dialog.component';
import type { CategoryDialogData } from './components/category-dialog/category-dialog.component';
import { SubcategoryDialogComponent } from './components/subcategory-dialog/subcategory-dialog.component';
import type { SubcategoryDialogData } from './components/subcategory-dialog/subcategory-dialog.component';
import { AddProductDialogComponent } from './components/add-product-dialog/add-product-dialog.component';
import { InventoryDialogsModule } from './inventory-dialogs.module';
import { CreateTransferDialogComponent } from './components/create-transfer-dialog/create-transfer-dialog.component';
import { InitialStockDialogComponent } from './components/initial-stock-dialog/initial-stock-dialog.component';
import { ReplenishStockDialogComponent } from './components/replenish-stock-dialog/replenish-stock-dialog.component';
import { InventoryDetailDialogComponent } from './components/inventory-detail-dialog/inventory-detail-dialog.component';
import { ViewTransferDialogComponent } from './components/view-transfer-dialog/view-transfer-dialog.component';
import { WarehouseSharingDialogComponent } from './components/warehouse-sharing-dialog/warehouse-sharing-dialog.component';
import { SubmitSupplierQuoteDialogComponent } from './components/submit-supplier-quote-dialog/submit-supplier-quote-dialog.component';
import { UserAddressCascadeFieldsComponent } from '../users/components/user-address-cascade-fields/user-address-cascade-fields.component';
import { SearchableProductPickerComponent } from './components/searchable-product-picker/searchable-product-picker.component';
import { SearchableWarehousePickerComponent } from './components/searchable-warehouse-picker/searchable-warehouse-picker.component';
import { EnRouteWarehouseStopsComponent } from './components/en-route-warehouse-stops/en-route-warehouse-stops.component';

/** SUPPLIER inventory module: products, warehouses, stock levels, and transfers. */
@NgModule({
  declarations: [
    InventoryWorkspaceComponent,
    CategoryDialogComponent,
    SubcategoryDialogComponent,
    AddProductDialogComponent,
    CreateTransferDialogComponent,
    InitialStockDialogComponent,
    ReplenishStockDialogComponent,
    InventoryDetailDialogComponent,
    ViewTransferDialogComponent,
    WarehouseSharingDialogComponent,
    SubmitSupplierQuoteDialogComponent,
    InventoryIntegrationApiPageComponent,
    InventoryIntegrationSubnavComponent,
    InventoryIntegrationSetupPanelComponent,
    LogisticsRouteJourneyComponent,
    EditTransferDialogComponent,
    EnRouteWarehouseStopsComponent,
  ],
  imports: [
    SharedModule,
    InventoryRoutingModule,
    InventoryDialogsModule,
    UserAddressCascadeFieldsComponent,
    SearchableProductPickerComponent,
    SearchableWarehousePickerComponent,
  ],
})
export class InventoryModule {}
