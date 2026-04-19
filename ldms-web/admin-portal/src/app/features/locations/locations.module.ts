import { NgModule } from '@angular/core';
import { LocationFormDialogComponent } from './components/location-form-dialog/location-form-dialog.component';
import { LocationTablePageComponent } from './components/location-table-page/location-table-page.component';
import { LocationsRoutingModule } from './locations-routing.module';
import { AdminLevelsComponent } from './pages/admin-levels/admin-levels.component';
import { CitiesComponent } from './pages/cities/cities.component';
import { CountriesComponent } from './pages/countries/countries.component';
import { DistrictsComponent } from './pages/districts/districts.component';
import { ProvincesComponent } from './pages/provinces/provinces.component';
import { SuburbsComponent } from './pages/suburbs/suburbs.component';
import { VillagesComponent } from './pages/villages/villages.component';
import { SharedModule } from '@shared/shared.module';

@NgModule({
  declarations: [
    LocationTablePageComponent,
    LocationFormDialogComponent,
    CountriesComponent,
    ProvincesComponent,
    DistrictsComponent,
    CitiesComponent,
    SuburbsComponent,
    VillagesComponent,
    AdminLevelsComponent,
  ],
  imports: [SharedModule, LocationsRoutingModule],
})
export class LocationsModule {}
