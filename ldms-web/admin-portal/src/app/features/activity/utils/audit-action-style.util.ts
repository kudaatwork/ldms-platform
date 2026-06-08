/** Classifies audited action codes for visual emphasis in Login & activity views. */
export type AuditActionKind = 'create' | 'update' | 'delete' | 'other';

export function auditActionKind(action: string | null | undefined): AuditActionKind {
  const code = String(action ?? '').trim().toUpperCase();
  if (!code) {
    return 'other';
  }
  if (code.startsWith('CREATE_') || code === 'CREATE') {
    return 'create';
  }
  if (code.startsWith('UPDATE_') || code.startsWith('EDIT_') || code === 'UPDATE' || code === 'EDIT') {
    return 'update';
  }
  if (code.startsWith('DELETE_') || code === 'DELETE') {
    return 'delete';
  }
  return 'other';
}

export function auditActionPillClass(action: string | null | undefined): string {
  const kind = auditActionKind(action);
  if (kind === 'create') {
    return 'lx-audit-action-pill lx-audit-action-pill--create';
  }
  if (kind === 'update') {
    return 'lx-audit-action-pill lx-audit-action-pill--update';
  }
  if (kind === 'delete') {
    return 'lx-audit-action-pill lx-audit-action-pill--delete';
  }
  return 'lx-audit-action-pill lx-audit-action-pill--neutral';
}

export function auditActionRowClass(action: string | null | undefined): string {
  const kind = auditActionKind(action);
  if (kind === 'create') {
    return 'lx-audit-row--create';
  }
  if (kind === 'update') {
    return 'lx-audit-row--update';
  }
  if (kind === 'delete') {
    return 'lx-audit-row--delete';
  }
  return '';
}

export function auditActionKindBadge(action: string | null | undefined): string {
  const kind = auditActionKind(action);
  if (kind === 'create') {
    return 'Created';
  }
  if (kind === 'update') {
    return 'Updated';
  }
  if (kind === 'delete') {
    return 'Deleted';
  }
  return '';
}

export function humanizeAuditAction(action: string | null | undefined): string {
  const code = String(action ?? '').trim();
  if (!code) {
    return '—';
  }
  return code.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
}
