import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AdminLevelsComponent } from './pages/admin-levels/admin-levels.component';
import { CitiesComponent } from './pages/cities/cities.component';
import { CountriesComponent } from './pages/countries/countries.component';
import { DistrictsComponent } from './pages/districts/districts.component';
import { ProvincesComponent } from './pages/provinces/provinces.component';
import { SuburbsComponent } from './pages/suburbs/suburbs.component';
import { VillagesComponent } from './pages/villages/villages.component';

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'countries' },
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
