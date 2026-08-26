#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOCAL_DATA_DIR="$PROJECT_ROOT/src/backend/.local-data"

detener() {
    local nombre="$1"
    local archivo_pid="$2"
    if [[ ! -f "$archivo_pid" ]]; then
        echo "$nombre no tiene un proceso local registrado."
        return
    fi

    local pid
    pid="$(cat "$archivo_pid")"
    if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
        kill "$pid"
        echo "$nombre local detenido."
    else
        echo "$nombre ya estaba detenido."
    fi
    rm -f "$archivo_pid"
}

detener "Backend" "$LOCAL_DATA_DIR/backend.pid"
detener "Frontend" "$LOCAL_DATA_DIR/frontend.pid"
