import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface GoogleAddressSuggestion {
  placeId: string;
  description: string;
  formattedAddress?: string;
  line1?: string;
  postalCode?: string;
}

declare global {
  interface Window {
    google?: any;
  }
}

@Injectable({ providedIn: 'root' })
export class GoogleAddressAutocompleteService {
  private loaderPromise: Promise<void> | null = null;

  search(input: string): Observable<GoogleAddressSuggestion[]> {
    if (!environment.googleAutocompleteEnabled || !environment.googlePlacesApiKey) {
      return of([]);
    }
    return new Observable<GoogleAddressSuggestion[]>((subscriber) => {
      this.ensureGoogleLoaded()
        .then(() => {
          const service = new window.google.maps.places.AutocompleteService();
          service.getPlacePredictions(
            { input, types: ['address'] },
            (predictions: any[] | null, status: string) => {
              if (status !== 'OK' || !Array.isArray(predictions)) {
                subscriber.next([]);
                subscriber.complete();
                return;
              }
              subscriber.next(
                predictions.map((p) => ({
                  placeId: String(p.place_id ?? ''),
                  description: String(p.description ?? ''),
                  formattedAddress: String(p.description ?? ''),
                  line1: String(p.structured_formatting?.main_text ?? ''),
                })),
              );
              subscriber.complete();
            },
          );
        })
        .catch(() => {
          subscriber.next([]);
          subscriber.complete();
        });
    });
  }

  private ensureGoogleLoaded(): Promise<void> {
    if (window.google?.maps?.places) {
      return Promise.resolve();
    }
    if (this.loaderPromise) {
      return this.loaderPromise;
    }
    this.loaderPromise = new Promise<void>((resolve, reject) => {
      const existing = document.getElementById('lx-google-places-script');
      if (existing) {
        existing.addEventListener('load', () => resolve());
        existing.addEventListener('error', () => reject(new Error('Google Places script failed to load')));
        return;
      }
      const script = document.createElement('script');
      script.id = 'lx-google-places-script';
      script.async = true;
      script.defer = true;
      script.src =
        'https://maps.googleapis.com/maps/api/js?libraries=places&key=' +
        encodeURIComponent(environment.googlePlacesApiKey);
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Google Places script failed to load'));
      document.head.appendChild(script);
    });
    return this.loaderPromise;
  }
}
