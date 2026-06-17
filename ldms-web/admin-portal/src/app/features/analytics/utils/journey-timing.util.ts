import type { TripLiveSnapshot } from '../models/shipment-live.model';

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

  if (days > 0) {
    return compact ? `${days}d ${hours}h` : `${days}d ${hours}h ${minutes}m`;
  }
  if (hours > 0) {
    return compact ? `${hours}h ${minutes}m` : `${hours}h ${minutes}m`;
  }
  if (minutes > 0) {
    return compact ? `${minutes}m` : `${minutes}m`;
  }
  return compact ? '<1m' : '0m';
}

function formatClockDuration(seconds: number): string {
  const safe = Math.max(0, Math.floor(seconds));
  const hours = Math.floor(safe / 3600);
  const minutes = Math.floor((safe % 3600) / 60);
  const secs = safe % 60;
  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }
  return `${minutes}:${String(secs).padStart(2, '0')}`;
}

function parseIsoMs(iso: string | undefined): number | null {
  if (!iso) {
    return null;
  }
  const ms = Date.parse(iso);
  return Number.isFinite(ms) ? ms : null;
}

function pct(part: number, total: number): number {
  if (total <= 0) {
    return 0;
  }
  return Math.min(100, Math.round((part / total) * 100));
}

function normalizePhase(raw: string | undefined): JourneyPhase {
  const upper = String(raw ?? 'IDLE').toUpperCase();
  if (upper === 'TRANSIT' || upper === 'WAITING' || upper === 'IDLE' || upper === 'COMPLETED') {
    return upper;
  }
  return 'IDLE';
}

export function buildJourneyTimeView(snapshot: TripLiveSnapshot | null, nowMs = Date.now()): JourneyTimeView {
  const startedMs = parseIsoMs(snapshot?.journeyStartedAt);

  let transit = snapshot?.transitSeconds ?? 0;
  let waiting = snapshot?.waitingSeconds ?? 0;
  let total = snapshot?.totalElapsedSeconds ?? 0;

  if (startedMs != null) {
    const elapsed = Math.max(0, Math.floor((nowMs - startedMs) / 1000));
    total = Math.max(total, elapsed);
  }

  if (snapshot?.journeyPhase === 'TRANSIT' && snapshot.lastTimingTickMs) {
    transit += Math.max(0, Math.floor((nowMs - snapshot.lastTimingTickMs) / 1000));
  } else if (snapshot?.journeyPhase === 'WAITING' && snapshot.lastTimingTickMs) {
    waiting += Math.max(0, Math.floor((nowMs - snapshot.lastTimingTickMs) / 1000));
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
  };
}

export function statusLabel(status: string): string {
  const labels: Record<string, string> = {
    PENDING_ALLOCATION: 'Pending allocation',
    PENDING_FLEET_ALLOCATION: 'Pending fleet',
    ALLOCATED: 'Allocated',
    IN_TRANSIT: 'In transit',
    ARRIVED_PENDING_OTP: 'At destination',
    DELIVERED: 'Delivered',
    CANCELLED: 'Cancelled',
    AT_BORDER: 'At border',
  };
  return labels[status] ?? status.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
}

export function fuelTone(fuelLevelPct: number | undefined): 'ok' | 'warn' | 'critical' | 'unknown' {
  if (fuelLevelPct == null || Number.isNaN(fuelLevelPct)) {
    return 'unknown';
  }
  if (fuelLevelPct <= 15) {
    return 'critical';
  }
  if (fuelLevelPct <= 30) {
    return 'warn';
  }
  return 'ok';
}
