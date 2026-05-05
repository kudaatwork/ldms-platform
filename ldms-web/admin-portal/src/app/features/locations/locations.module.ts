import { NgModule } from '@angular/core';
import { LocationsService } from './services/locations.service';
import { LocationFormDialogComponent } from './components/location-form-dialog/location-form-dialog.component';
import { LocationTablePageComponent } from './components/location-table-page/location-table-page.component';
import { DeleteConfirmDialogComponent } from './components/delete-confirm-dialog/delete-confirm-dialog.component';
import { LocationsRoutingModule } from './locations-routing.module';
import { AdminLevelsComponent } from './pages/admin-levels/admin-levels.component';
import { AddressesComponent } from './pages/addresses/addresses.component';
import { CitiesComponent } from './pages/cities/cities.component';
import { CountriesComponent } from './pages/countries/countries.component';
import { DistrictsComponent } from './pages/districts/districts.component';
import { ProvincesComponent } from './pages/provinces/provinces.component';
import { SuburbsComponent } from './pages/suburbs/suburbs.component';
import { VillagesComponent } from './pages/villages/villages.component';
import { LanguagesComponent } from './pages/languages/languages.component';
import { LocalizedNamesComponent } from './pages/localized-names/localized-names.component';
import { LocationExplorerComponent } from './pages/location-explorer/location-explorer.component';
import { SharedModule } from '@shared/shared.module';

@NgModule({
  declarations: [
    LocationTablePageComponent,
    LocationFormDialogComponent,
    DeleteConfirmDialogComponent,
    LocationExplorerComponent,
    CountriesComponent,
    ProvincesComponent,
    DistrictsComponent,
    CitiesComponent,
    AddressesComponent,
    SuburbsComponent,
    VillagesComponent,
    AdminLevelsComponent,
    LanguagesComponent,
    LocalizedNamesComponent,
  ],
  imports: [SharedModule, LocationsRoutingModule],
})
export class LocationsModule {
  /**
   * As soon as the user enters the locations area, kick off all FK list calls
   * so the Add/Edit dialog opens against a primed shareReplay cache instead of
   * spinning while HTTP returns. Subsequent calls are instant.
   */
  constructor(locations: LocationsService) {
    locations.prefetchAllDialogOptions().subscribe({ error: () => void 0 });
  }
}
