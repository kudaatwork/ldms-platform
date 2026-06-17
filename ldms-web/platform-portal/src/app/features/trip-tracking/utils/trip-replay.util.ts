import type { TripLiveSnapshot, TripRouteWaypoint, TripTimelineEvent } from '../models/trip-tracking.model';

export interface TripReplayFrame {
  lat: number;
  lng: number;
  speedKmh: number;
  headingDeg: number;
  progressPct: number;
  atMs: number;
}

export type TripReplaySpeed = 0.25 | 0.5 | 1 | 2 | 4 | 8 | 16;

export const TRIP_REPLAY_SPEED_OPTIONS: TripReplaySpeed[] = [0.25, 0.5, 1, 2, 4, 8, 16];

/** Build ordered playback frames from server trail + route waypoints. */
export function buildTripReplayFrames(
  snapshot: TripLiveSnapshot | null,
  events: TripTimelineEvent[],
): TripReplayFrame[] {
  const trail = (snapshot?.trail ?? []).filter((p) => p.latitude && p.longitude);
  const route = (snapshot?.routeWaypoints ?? []).filter((p) => p.latitude && p.longitude);
  const source = trail.length >= 2 ? trail : route.length >= 2 ? route : [];

  if (!source.length) {
    if (snapshot?.latitude != null && snapshot?.longitude != null) {
      return [
        {
          lat: snapshot.latitude,
          lng: snapshot.longitude,
          speedKmh: snapshot.speedKmh ?? 0,
          headingDeg: snapshot.headingDeg ?? 0,
          progressPct: snapshot.overallProgressPct ?? 0,
          atMs: Date.now(),
        },
      ];
    }
    return [];
  }

  const startMs = parseStartMs(snapshot, events);
  const endMs = parseEndMs(snapshot, events, startMs, source.length);
  const spanMs = Math.max(endMs - startMs, source.length * 2000);

  const frames: TripReplayFrame[] = source.map((point, index) => {
    const atMs = resolvePointTimeMs(point, index, source.length, startMs, spanMs);
    return {
      lat: point.latitude,
      lng: point.longitude,
      speedKmh: point.speedKmh ?? inferSpeed(source, index),
      headingDeg: inferHeading(source, index),
      progressPct: inferProgress(source, index, snapshot?.overallProgressPct),
      atMs,
    };
  });

  return frames.sort((a, b) => a.atMs - b.atMs);
}

export function formatReplayClock(ms: number): string {
  const totalSec = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(totalSec / 3600);
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;
  if (h > 0) {
    return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
  return `${m}:${String(s).padStart(2, '0')}`;
}

export function interpolateReplayFrame(frames: TripReplayFrame[], atMs: number): TripReplayFrame | null {
  if (!frames.length) {
    return null;
  }
  if (atMs <= frames[0].atMs) {
    return frames[0];
  }
  const last = frames[frames.length - 1];
  if (atMs >= last.atMs) {
    return last;
  }
  for (let i = 0; i < frames.length - 1; i++) {
    const a = frames[i];
    const b = frames[i + 1];
    if (atMs >= a.atMs && atMs <= b.atMs) {
      const t = (atMs - a.atMs) / Math.max(1, b.atMs - a.atMs);
      return {
        lat: a.lat + (b.lat - a.lat) * t,
        lng: a.lng + (b.lng - a.lng) * t,
        speedKmh: a.speedKmh + (b.speedKmh - a.speedKmh) * t,
        headingDeg: a.headingDeg,
        progressPct: a.progressPct + (b.progressPct - a.progressPct) * t,
        atMs,
      };
    }
  }
  return last;
}

export function replayIndexForMs(frames: TripReplayFrame[], atMs: number): number {
  if (!frames.length) {
    return 0;
  }
  let idx = 0;
  for (let i = 0; i < frames.length; i++) {
    if (frames[i].atMs <= atMs) {
      idx = i;
    } else {
      break;
    }
  }
  return idx;
}

function parseStartMs(snapshot: TripLiveSnapshot | null, events: TripTimelineEvent[]): number {
  if (snapshot?.journeyStartedAt) {
    const parsed = Date.parse(snapshot.journeyStartedAt);
    if (!Number.isNaN(parsed)) {
      return parsed;
    }
  }
  const departed = events.find((e) => ['DEPARTED', 'DEPARTURE'].includes(String(e.eventType).toUpperCase()));
  if (departed?.recordedAtIso) {
    const parsed = Date.parse(departed.recordedAtIso);
    if (!Number.isNaN(parsed)) {
      return parsed;
    }
  }
  return Date.now() - (snapshot?.totalElapsedSeconds ?? 3600) * 1000;
}

function parseEndMs(
  snapshot: TripLiveSnapshot | null,
  events: TripTimelineEvent[],
  startMs: number,
  pointCount: number,
): number {
  const delivered = [...events]
    .reverse()
    .find((e) => ['DELIVERED', 'ARRIVED', 'ARRIVAL'].includes(String(e.eventType).toUpperCase()));
  if (delivered?.recordedAtIso) {
    const parsed = Date.parse(delivered.recordedAtIso);
    if (!Number.isNaN(parsed) && parsed > startMs) {
      return parsed;
    }
  }
  if (snapshot?.totalElapsedSeconds) {
    return startMs + snapshot.totalElapsedSeconds * 1000;
  }
  return startMs + pointCount * 2000;
}

function resolvePointTimeMs(
  point: TripRouteWaypoint,
  index: number,
  total: number,
  startMs: number,
  spanMs: number,
): number {
  if (point.recordedAt) {
    const parsed = Date.parse(point.recordedAt);
    if (!Number.isNaN(parsed)) {
      return parsed;
    }
  }
  const ratio = total <= 1 ? 0 : index / (total - 1);
  return startMs + spanMs * ratio;
}

function inferSpeed(source: TripRouteWaypoint[], index: number): number {
  const direct = source[index]?.speedKmh;
  if (direct != null && direct > 0) {
    return direct;
  }
  if (index <= 0) {
    return 0;
  }
  const prev = source[index - 1];
  const curr = source[index];
  const dt = 2;
  const distKm = haversineKm(prev.latitude, prev.longitude, curr.latitude, curr.longitude);
  return distKm > 0 ? (distKm / dt) * 3600 : 0;
}

function inferHeading(source: TripRouteWaypoint[], index: number): number {
  const next = source[Math.min(index + 1, source.length - 1)];
  const curr = source[index];
  if (!next || next === curr) {
    return 0;
  }
  return bearingDeg(curr.latitude, curr.longitude, next.latitude, next.longitude);
}

function inferProgress(source: TripRouteWaypoint[], index: number, fallback?: number): number {
  if (fallback != null && source.length <= 1) {
    return fallback;
  }
  return source.length <= 1 ? 0 : Math.round((index / (source.length - 1)) * 100);
}

function haversineKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const r = 6371;
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function bearingDeg(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const dLng = toRad(lng2 - lng1);
  const y = Math.sin(dLng) * Math.cos(toRad(lat2));
  const x =
    Math.cos(toRad(lat1)) * Math.sin(toRad(lat2)) -
    Math.sin(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.cos(dLng);
  return (toDeg(Math.atan2(y, x)) + 360) % 360;
}

function toRad(v: number): number {
  return (v * Math.PI) / 180;
}

function toDeg(v: number): number {
  return (v * 180) / Math.PI;
}
