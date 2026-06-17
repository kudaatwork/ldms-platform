#!/usr/bin/env bash
# Load GEMINI_* vars for local terminal sessions (gitignored .env at repo root).
# Usage: source scripts/dev-gemini-env.sh

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT}/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE} — create it with GEMINI_API_KEY=..." >&2
  return 1 2>/dev/null || exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

echo "GEMINI_API_KEY is set (${#GEMINI_API_KEY} chars). Model: ${GEMINI_MODEL:-gemini-2.5-flash}"
