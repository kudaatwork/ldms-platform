/** Builds dashboard greeting: "Welcome {firstName}". */
export function formatWelcomeMessage(input?: {
  firstName?: string | null;
  displayName?: string | null;
  email?: string | null;
} | null): string {
  if (!input) {
    return 'Welcome';
  }
  const first = String(input.firstName ?? '').trim();
  if (first && first !== 'User') {
    return `Welcome ${first}`;
  }
  const fromDisplay = String(input.displayName ?? '')
    .trim()
    .split(/\s+/)[0];
  if (fromDisplay && fromDisplay !== 'User') {
    return `Welcome ${fromDisplay}`;
  }
  const email = String(input.email ?? '').trim();
  const local = email.includes('@') ? email.split('@')[0] : '';
  if (local && local !== 'User') {
    return `Welcome ${local}`;
  }
  return 'Welcome';
}
