/** Display name for the LDMS platform assistant (messaging-bot persona). */
export const LEXXI_BOT_NAME = 'Lexxi';

export const LEXXI_DEFAULT_TOPIC = 'Chat with Lexxi';

/** Guest landing topic for human support (not Lexxi AI). */
export const GUEST_LIVE_CHAT_TOPIC = 'Live chat';

export function lexxiDisplayName(agentMode = false): string {
  return agentMode ? `${LEXXI_BOT_NAME} (Agent)` : LEXXI_BOT_NAME;
}
