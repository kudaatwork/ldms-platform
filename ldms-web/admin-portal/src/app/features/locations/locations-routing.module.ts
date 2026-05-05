import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
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

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'countries' },
  {
    path: 'explorer',
    component: LocationExplorerComponent,
    data: { breadcrumb: 'Hierarchy explorer' },
  },
  {
    path: 'countries',
    component: CountriesComponent,
    data: { breadcrumb: 'Countries' },
  },
  {
    path: 'provinces',
    component: ProvincesComponent,
    data: { breadcrumb: 'Provinces' },
  },
  {
    path: 'districts',
    component: DistrictsComponent,
    data: { breadcrumb: 'Districts' },
  },
  {
    path: 'cities',
    component: CitiesComponent,
    data: { breadcrumb: 'Cities' },
  },
  {
    path: 'addresses',
    component: AddressesComponent,
    data: { breadcrumb: 'Addresses' },
  },
  {
    path: 'languages',
    component: LanguagesComponent,
    data: { breadcrumb: 'Languages' },
  },
  {
    path: 'localized-names',
    component: LocalizedNamesComponent,
    data: { breadcrumb: 'Localized names' },
  },
  {
    path: 'suburbs',
    component: SuburbsComponent,
    data: { breadcrumb: 'Suburbs' },
  },
  {
    path: 'villages',
    component: VillagesComponent,
    data: { breadcrumb: 'Villages' },
  },
  {
    path: 'admin-levels',
    component: AdminLevelsComponent,
    data: { breadcrumb: 'Administrative levels' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class LocationsRoutingModule {}
