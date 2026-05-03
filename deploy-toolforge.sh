#!/usr/bin/env bash
set -euo pipefail

REPO="$1"
REF="$2"

EXTRA_MSG="${3:-}"

log_formatted() {
    local base_msg="$1"
    if [ -n "$EXTRA_MSG" ]; then
        dologmsg "$base_msg ($EXTRA_MSG)"
    else
        dologmsg "$base_msg"
    fi
}

log_formatted "[DEPLOY] Starting deployment  | ref=$REF"

toolforge build start "$REPO" --ref "$REF"

log_formatted "[DEPLOY] Build triggered successfully | ref=$REF"

log_formatted "[DEPLOY] Restarting buildservice | ref=$REF"

toolforge webservice buildservice restart

log_formatted "[DEPLOY] Deployment completed successfully | ref=$REF"