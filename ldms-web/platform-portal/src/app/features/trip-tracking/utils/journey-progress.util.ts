import type { TripLiveSnapshot, TripRouteWaypoint } from '../models/trip-tracking.model';

export type JourneyLegStatus = 'completed' | 'current' | 'pending';

export interface JourneyLegView {
  index: number;
  label: string;
  type: string;
  status: JourneyLegStatus;
  statusLabel: string;
}

export interface JourneyProgressView {
  legs: JourneyLegView[];
  completedCount: number;
  totalCount: number;
  completionLabel: string;
  currentLegLabel: string;
  segmentProgressPct: number;
}

export function buildJourneyProgressView(snapshot: TripLiveSnapshot | null): JourneyProgressView {
  const waypoints = snapshot?.routeWaypoints ?? [];
  const totalCount = snapshot?.totalWaypointCount ?? waypoints.length;
  const completedCount = resolveCompletedCount(snapshot, waypoints, totalCount);
  const segmentIndex = snapshot?.currentSegmentIndex ?? 0;
  const segmentProgressPct = snapshot?.segmentProgressPct ?? 0;
  const overall = snapshot?.overallProgressPct ?? 0;
  const allComplete = overall >= 100 || completedCount >= totalCount;

  const legs = waypoints.map((wp, index) => {
    const status = resolveLegStatus(index, segmentIndex, allComplete, totalCount);
    return {
      index,
      label: wp.label,
      type: wp.type,
      status,
      statusLabel: legStatusLabel(status),
    };
  });

  const currentLeg = legs.find((l) => l.status === 'current');

  return {
    legs,
    completedCount,
    totalCount,
    completionLabel: totalCount > 0 ? `${completedCount} of ${totalCount} checkpoints done` : 'No corridor checkpoints',
    currentLegLabel: currentLeg?.label ?? (allComplete && totalCount ? waypoints[totalCount - 1]?.label ?? 'Destination' : '—'),
    segmentProgressPct,
  };
}

function resolveCompletedCount(
  snapshot: TripLiveSnapshot | null,
  waypoints: TripRouteWaypoint[],
  totalCount: number,
): number {
  if (snapshot?.completedWaypointCount != null && snapshot.completedWaypointCount >= 0) {
    return Math.min(snapshot.completedWaypointCount, totalCount || snapshot.completedWaypointCount);
  }
  const overall = snapshot?.overallProgressPct ?? 0;
  if (overall >= 100) {
    return totalCount;
  }
  const segmentIndex = snapshot?.currentSegmentIndex ?? 0;
  if (!waypoints.length) {
    return 0;
  }
  return Math.min(Math.max(segmentIndex, 0) + 1, waypoints.length);
}

function resolveLegStatus(
  index: number,
  segmentIndex: number,
  allComplete: boolean,
  totalCount: number,
): JourneyLegStatus {
  if (totalCount <= 0) {
    return 'pending';
  }
  if (allComplete) {
    return 'completed';
  }
  if (index <= segmentIndex) {
    return 'completed';
  }
  if (index === segmentIndex + 1) {
    return 'current';
  }
  return 'pending';
}

function legStatusLabel(status: JourneyLegStatus): string {
  switch (status) {
    case 'completed':
      return 'Done';
    case 'current':
      return 'In progress';
    default:
      return 'Upcoming';
  }
}
