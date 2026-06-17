import type { TripLiveSnapshot, TripTimelineEvent } from '../models/trip-tracking.model';

export type JourneyPhase = 'TRANSIT' | 'WAITING' | 'IDLE' | 'COMPLETED';

export interface JourneyTimeView {
  totalSeconds: number;
  transitSeconds: number;
  waitingSeconds: number;
  idleSeconds: number;
  phase: JourneyPhase;
  phaseLabel: string;
  phaseHint: string;
  etaSeconds?: number;
  etaLabel: string;
  transitPct: number;
  waitingPct: number;
  idlePct: number;
  totalLabel: string;
  transitLabel: string;
  waitingLabel: string;
  idleLabel: string;
  startedAtLabel?: string;
  eventSegments: JourneyEventSegment[];
}

export interface JourneyEventSegment {
  label: string;
  durationSeconds: number;
  durationLabel: string;
  tone: 'transit' | 'waiting' | 'idle' | 'milestone';
}

const PHASE_COPY: Record<JourneyPhase, { label: string; hint: string }> = {
  TRANSIT: { label: 'In transit', hint: 'Vehicle is moving along the corridor' },
  WAITING: { label: 'Waiting / halted', hint: 'Break, border hold, or roadside stop' },
  IDLE: { label: 'Standing by', hint: 'Trip started — awaiting movement or telematics' },
  COMPLETED: { label: 'Journey complete', hint: 'Trip has arrived or been closed' },
};

export function formatDuration(seconds: number, compact = false): string {
  const safe = Math.max(0, Math.floor(seconds));
  const days = Math.floor(safe / 86_400);
  const hours = Math.floor((safe % 86_400) / 3600);
  const minutes = Math.floor((safe % 3600) / 60);
  const secs = safe % 60;

  if (days > 0) {
    return compact ? `${days}d ${hours}h` : `${days}d ${hours}h ${minutes}m`;
  }
  if (hours > 0) {
    return compact ? `${hours}h ${minutes}m` : `${hours}h ${minutes}m ${secs}s`;
  }
  if (minutes > 0) {
    return compact ? `${minutes}m ${secs}s` : `${minutes}m ${secs}s`;
  }
  return `${secs}s`;
}

export function formatClockDuration(seconds: number): string {
  const safe = Math.max(0, Math.floor(seconds));
  const hours = Math.floor(safe / 3600);
  const minutes = Math.floor((safe % 3600) / 60);
  const secs = safe % 60;
  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }
  return `${minutes}:${String(secs).padStart(2, '0')}`;
}

function parseIsoMs(value: string | undefined): number | null {
  if (!value?.trim()) {
    return null;
  }
  const ms = Date.parse(value);
  return Number.isFinite(ms) ? ms : null;
}

function pct(part: number, total: number): number {
  if (total <= 0) {
    return 0;
  }
  return Math.min(100, Math.round((part / total) * 100));
}

export function buildJourneyTimeView(
  snapshot: TripLiveSnapshot | null,
  options?: {
    nowMs?: number;
    startedAtIso?: string;
    startedAtLabel?: string;
    timeline?: TripTimelineEvent[];
  },
): JourneyTimeView {
  const nowMs = options?.nowMs ?? Date.now();
  const startedMs =
    parseIsoMs(snapshot?.journeyStartedAt) ??
    parseIsoMs(options?.startedAtIso) ??
    null;

  let transit = snapshot?.transitSeconds ?? 0;
  let waiting = snapshot?.waitingSeconds ?? 0;
  let total = snapshot?.totalElapsedSeconds ?? 0;

  if (startedMs != null) {
    const elapsed = Math.max(0, Math.floor((nowMs - startedMs) / 1000));
    total = Math.max(total, elapsed);
  }

  if (snapshot?.journeyPhase === 'TRANSIT' && snapshot.lastTimingTickMs) {
    const extra = Math.max(0, Math.floor((nowMs - snapshot.lastTimingTickMs) / 1000));
    transit += extra;
  } else if (snapshot?.journeyPhase === 'WAITING' && snapshot.lastTimingTickMs) {
    const extra = Math.max(0, Math.floor((nowMs - snapshot.lastTimingTickMs) / 1000));
    waiting += extra;
  }

  const idle = Math.max(0, total - transit - waiting);
  const phase = normalizePhase(snapshot?.journeyPhase);
  const phaseCopy = PHASE_COPY[phase];
  const etaSeconds = snapshot?.estimatedArrivalSeconds;

  return {
    totalSeconds: total,
    transitSeconds: transit,
    waitingSeconds: waiting,
    idleSeconds: idle,
    phase,
    phaseLabel: phaseCopy.label,
    phaseHint: phaseCopy.hint,
    etaSeconds,
    etaLabel: etaSeconds != null && etaSeconds > 0 ? formatDuration(etaSeconds, true) : '—',
    transitPct: pct(transit, total),
    waitingPct: pct(waiting, total),
    idlePct: pct(idle, total),
    totalLabel: formatClockDuration(total),
    transitLabel: formatDuration(transit, true),
    waitingLabel: formatDuration(waiting, true),
    idleLabel: formatDuration(idle, true),
    startedAtLabel: options?.startedAtLabel,
    eventSegments: buildEventSegments(options?.timeline ?? [], nowMs),
  };
}

function normalizePhase(raw: string | undefined): JourneyPhase {
  const upper = String(raw ?? 'IDLE').toUpperCase();
  if (upper === 'TRANSIT' || upper === 'WAITING' || upper === 'IDLE' || upper === 'COMPLETED') {
    return upper;
  }
  return 'IDLE';
}

const WAIT_EVENT_TYPES = new Set([
  'DRIVER_BREAK',
  'ROADSIDE_FUEL_STOP',
  'ROADSIDE_MECHANIC_STOP',
  'ARRIVED_AT_BORDER',
]);

const RESUME_EVENT_TYPES = new Set(['DRIVER_RESUMED', 'ROADSIDE_RESUMED', 'BORDER_CLEARED']);

function buildEventSegments(events: TripTimelineEvent[], nowMs: number): JourneyEventSegment[] {
  const sorted = [...events]
    .map((e) => ({
      ...e,
      ms: parseIsoMs(e.recordedAtIso) ?? 0,
    }))
    .filter((e) => e.ms > 0)
    .sort((a, b) => a.ms - b.ms);

  const segments: JourneyEventSegment[] = [];
  let waitStart: { ms: number; label: string } | null = null;

  for (const event of sorted) {
    const type = String(event.eventType).toUpperCase();
    if (WAIT_EVENT_TYPES.has(type)) {
      waitStart = { ms: event.ms, label: event.eventTypeLabel };
      continue;
    }
    if (waitStart && RESUME_EVENT_TYPES.has(type)) {
      const duration = Math.max(0, Math.floor((event.ms - waitStart.ms) / 1000));
      if (duration > 0) {
        segments.push({
          label: waitStart.label,
          durationSeconds: duration,
          durationLabel: formatDuration(duration, true),
          tone: 'waiting',
        });
      }
      waitStart = null;
      continue;
    }
    if (type === 'DEPARTED' || type === 'ARRIVED') {
      segments.push({
        label: event.eventTypeLabel,
        durationSeconds: 0,
        durationLabel: event.recordedAtLabel,
        tone: 'milestone',
      });
    }
  }

  if (waitStart) {
    const duration = Math.max(0, Math.floor((nowMs - waitStart.ms) / 1000));
    segments.push({
      label: `${waitStart.label} (ongoing)`,
      durationSeconds: duration,
      durationLabel: formatDuration(duration, true),
      tone: 'waiting',
    });
  }

  return segments.slice(-6);
}

export function journeyPhaseIcon(phase: JourneyPhase): string {
  switch (phase) {
    case 'TRANSIT':
      return 'local_shipping';
    case 'WAITING':
      return 'hourglass_top';
    case 'COMPLETED':
      return 'flag';
    default:
      return 'schedule';
  }
}
