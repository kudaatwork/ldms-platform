/** Y-axis tick marks for the session volume column chart (top → bottom). */
export function buildSessionVolumeYAxisTicks(maxCount: number): number[] {
  const max = Math.max(maxCount, 1);
  let step = 1;
  if (max > 5) {
    step = 2;
  }
  if (max > 12) {
    step = 5;
  }
  if (max > 30) {
    step = 10;
  }
  if (max > 100) {
    step = Math.ceil(max / 5 / 10) * 10;
  }

  const ticks: number[] = [];
  for (let value = 0; value <= max; value += step) {
    ticks.push(value);
  }
  const top = ticks[ticks.length - 1];
  if (top < max) {
    ticks.push(top + step);
  }
  return ticks.reverse();
}

export interface MessageChartDayRow {
  date: string;
  label: string;
  userMessages: number;
  botMessages: number;
  totalMessages: number;
}

export function buildMessageChartRows(
  messagesByDay: Array<{ date: string; userMessages: number; botMessages: number }>,
  formatDate: (iso: string) => string,
  maxDays = 14,
): MessageChartDayRow[] {
  return messagesByDay.slice(-maxDays).map((row) => ({
    date: row.date,
    label: formatDate(row.date),
    userMessages: row.userMessages ?? 0,
    botMessages: row.botMessages ?? 0,
    totalMessages: (row.userMessages ?? 0) + (row.botMessages ?? 0),
  }));
}

/** SVG ring circumference for r=48 in the 120×120 viewBox. */
export const BOT_MIX_RING_CIRCUMFERENCE = 301.59;

export function botMixRingDash(sharePct: number): string {
  const len = Math.max(0, (sharePct / 100) * BOT_MIX_RING_CIRCUMFERENCE);
  return `${len} ${BOT_MIX_RING_CIRCUMFERENCE}`;
}

export function botMixRingOffset(userSharePct: number): number {
  return -Math.max(0, (userSharePct / 100) * BOT_MIX_RING_CIRCUMFERENCE);
}
