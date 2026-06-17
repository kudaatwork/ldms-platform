export type LxMapMarkerTone = 'primary' | 'warning' | 'secondary';

export interface LxMapMarker {
  id: string | number;
  lat: number;
  lng: number;
  label?: string;
  tone?: LxMapMarkerTone;
  headingDeg?: number;
}

export interface LxMapLatLng {
  lat: number;
  lng: number;
}

export interface LxMapWaypoint extends LxMapLatLng {
  label: string;
  type?: string;
}

export type LxMapStyle = 'standard' | 'satellite' | 'live';
