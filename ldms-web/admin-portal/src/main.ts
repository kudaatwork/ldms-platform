import { platformBrowser } from '@angular/platform-browser';
import { Chart, registerables } from 'chart.js';
import { AppModule } from './app/app.module';

Chart.register(...registerables);

platformBrowser().bootstrapModule(AppModule, {
  ngZoneEventCoalescing: true,
})
  .catch(err => console.error(err));
