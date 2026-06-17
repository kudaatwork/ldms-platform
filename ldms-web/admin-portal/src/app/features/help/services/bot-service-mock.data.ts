export type BotSessionStatus = 'ACTIVE' | 'WAITING' | 'RESOLVED' | 'ESCALATED';

export interface BotMessage {
  id: string;
  role: 'user' | 'bot' | 'system';
  body: string;
  sentAt: string;
}

export interface BotConversationSession {
  sessionId: string;
  userDisplayName: string;
  userPhone: string;
  organizationName: string;
  channel: 'WHATSAPP' | 'WEB' | 'SMS';
  status: BotSessionStatus;
  statusLabel: string;
  topic: string;
  lastMessageAt: string;
  messageCount: number;
  satisfactionScore?: number;
  messages: BotMessage[];
}

export const BOT_CONVERSATION_SEEDS: BotConversationSession[] = [
  {
    sessionId: 'bot-7f3a',
    userDisplayName: 'Tendai Moyo',
    userPhone: '+263 77 234 5678',
    organizationName: 'ZimChem Logistics',
    channel: 'WHATSAPP',
    status: 'ACTIVE',
    statusLabel: 'Active',
    topic: 'Trip stop at border',
    lastMessageAt: '2026-06-17T10:08:00',
    messageCount: 12,
    messages: [
      { id: 'm1', role: 'user', body: '1', sentAt: '2026-06-17T10:02:00' },
      {
        id: 'm2',
        role: 'bot',
        body: 'Recorded: you are at Beitbridge border post for shipment SHP-20261012. ETA to destination updated to 18:30.',
        sentAt: '2026-06-17T10:02:04',
      },
      { id: 'm3', role: 'user', body: 'Any docs needed?', sentAt: '2026-06-17T10:05:00' },
      {
        id: 'm4',
        role: 'bot',
        body: 'Your clearing agent uploaded the customs release. Driver app will show green clearance in ~2 min.',
        sentAt: '2026-06-17T10:05:03',
      },
    ],
  },
  {
    sessionId: 'bot-2c91',
    userDisplayName: 'Sarah Ncube',
    userPhone: '+263 71 890 1234',
    organizationName: 'Bulawayo Retail Group',
    channel: 'WHATSAPP',
    status: 'WAITING',
    statusLabel: 'Awaiting reply',
    topic: 'Fuel advance request',
    lastMessageAt: '2026-06-17T09:55:00',
    messageCount: 8,
    messages: [
      { id: 'm1', role: 'user', body: 'Need fuel advance for trip TR-8842', sentAt: '2026-06-17T09:50:00' },
      {
        id: 'm2',
        role: 'bot',
        body: 'I can start a fuel fund request. Reply with litres needed (max 400L per policy) or type CANCEL.',
        sentAt: '2026-06-17T09:50:02',
      },
      { id: 'm3', role: 'user', body: '250', sentAt: '2026-06-17T09:55:00' },
    ],
  },
  {
    sessionId: 'bot-9e44',
    userDisplayName: 'Peter Dube',
    userPhone: '+263 78 456 7890',
    organizationName: 'CrossBorder Hauliers',
    channel: 'WHATSAPP',
    status: 'RESOLVED',
    statusLabel: 'Resolved',
    topic: 'GRV QR code',
    lastMessageAt: '2026-06-17T08:40:00',
    messageCount: 6,
    satisfactionScore: 5,
    messages: [
      { id: 'm1', role: 'user', body: 'Send GRV QR for delivery', sentAt: '2026-06-17T08:35:00' },
      {
        id: 'm2',
        role: 'bot',
        body: 'GRV token GRV-9921 sent. Receiver can scan at Mutare warehouse gate B.',
        sentAt: '2026-06-17T08:35:05',
      },
      { id: 'm3', role: 'user', body: 'Thanks', sentAt: '2026-06-17T08:40:00' },
      { id: 'm4', role: 'bot', body: 'Glad to help. Safe travels!', sentAt: '2026-06-17T08:40:02' },
    ],
  },
  {
    sessionId: 'bot-1a77',
    userDisplayName: 'Linda Chikwanha',
    userPhone: '+263 73 112 3344',
    organizationName: 'Mutare Agro Supplies',
    channel: 'WEB',
    status: 'ESCALATED',
    statusLabel: 'Escalated to agent',
    topic: 'Invoice dispute',
    lastMessageAt: '2026-06-17T07:20:00',
    messageCount: 15,
    messages: [
      { id: 'm1', role: 'user', body: 'Invoice INV-4402 amount looks wrong', sentAt: '2026-06-17T07:10:00' },
      {
        id: 'm2',
        role: 'bot',
        body: 'I see INV-4402 for USD 4,280. Would you like a line-item breakdown or to speak with billing?',
        sentAt: '2026-06-17T07:10:04',
      },
      { id: 'm3', role: 'user', body: 'Speak with billing', sentAt: '2026-06-17T07:15:00' },
      {
        id: 'm4',
        role: 'system',
        body: 'Session escalated to Help & Support queue. Ticket #HS-2044 created.',
        sentAt: '2026-06-17T07:15:01',
      },
    ],
  },
  {
    sessionId: 'bot-5d22',
    userDisplayName: 'James Muzenda',
    userPhone: '+263 77 998 7766',
    organizationName: 'Harare Fuel Network',
    channel: 'SMS',
    status: 'ACTIVE',
    statusLabel: 'Active',
    topic: 'Station locator',
    lastMessageAt: '2026-06-17T10:00:00',
    messageCount: 4,
    messages: [
      { id: 'm1', role: 'user', body: 'Nearest station on A5', sentAt: '2026-06-17T09:58:00' },
      {
        id: 'm2',
        role: 'bot',
        body: 'HF Network Norton depot — 12 km ahead, diesel available, queue ~8 min.',
        sentAt: '2026-06-17T09:58:06',
      },
    ],
  },
];
