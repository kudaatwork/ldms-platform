#!/usr/bin/env bash
# Backward-compatible alias — prefer scripts/dev-llm-env.sh
# Usage: source scripts/dev-gemini-env.sh

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
source "${ROOT}/scripts/dev-llm-env.sh"
