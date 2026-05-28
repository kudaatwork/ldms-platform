/** Builds dashboard greeting: "Welcome {firstName}". */
export function formatWelcomeMessage(input?: {
  firstName?: string | null;
  displayName?: string | null;
  email?: string | null;
  username?: string | null;
} | null): string {
  if (!input) {
    return 'Welcome';
  }
  const first = String(input.firstName ?? '').trim();
  if (first && first.toLowerCase() !== 'user') {
    return `Welcome ${first}`;
  }
  return 'Welcome';
}
