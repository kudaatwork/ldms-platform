#!/usr/bin/env bash
# Load LLM provider API keys for local terminal sessions (gitignored .env at repo root).
# Usage: source scripts/dev-llm-env.sh

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT}/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE} — create it with ANTHROPIC_API_KEY and/or GEMINI_API_KEY" >&2
  return 1 2>/dev/null || exit 1
fi

set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

PROVIDER="${BOT_LLM_PROVIDER:-auto}"
echo "BOT_LLM_PROVIDER=${PROVIDER}"
if [[ -n "${ANTHROPIC_API_KEY:-}" ]]; then
  echo "ANTHROPIC_API_KEY is set (${#ANTHROPIC_API_KEY} chars). Model: ${ANTHROPIC_MODEL:-claude-sonnet-4-6}"
fi
if [[ -n "${GEMINI_API_KEY:-}" ]]; then
  echo "GEMINI_API_KEY is set (${#GEMINI_API_KEY} chars). Model: ${GEMINI_MODEL:-gemini-2.5-flash}"
fi
if [[ -z "${ANTHROPIC_API_KEY:-}" && -z "${GEMINI_API_KEY:-}" ]]; then
  echo "Warning: no LLM API keys found in .env — bot will use keyword fallbacks only." >&2
fi
