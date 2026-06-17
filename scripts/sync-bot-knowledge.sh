#!/usr/bin/env bash
# Sync LDMS documentation into the messaging-bot knowledge corpus.
# Run after updating docs/ or before restarting ldms-messaging-bot.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/ldms-backend/ldms-messaging-bot/src/main/resources/ldms-knowledge"

mkdir -p "$DEST"
cp "$ROOT/docs/LDMS-SYSTEM-ARCHITECTURE.md" "$DEST/ldms-system-architecture.md"

echo "Synced knowledge to $DEST"
echo "Restart ldms-messaging-bot or POST /ldms-messaging-inbound/v1/backoffice/bot-knowledge/reload"
