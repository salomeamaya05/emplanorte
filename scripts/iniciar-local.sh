#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/src/backend"
FRONTEND_DIR="$PROJECT_ROOT/src/frontend"
LOCAL_DATA_DIR="$BACKEND_DIR/.local-data"
LOCAL_ENV_FILE="$BACKEND_DIR/.env.localhost"
BACKEND_PID_FILE="$LOCAL_DATA_DIR/backend.pid"
FRONTEND_PID_FILE="$LOCAL_DATA_DIR/frontend.pid"
BACKEND_LOG="$LOCAL_DATA_DIR/backend.log"
FRONTEND_LOG="$LOCAL_DATA_DIR/frontend.log"
FOREGROUND=false
STARTUP_TIMEOUT_SECONDS="${EMPLANORTE_LOCAL_STARTUP_TIMEOUT_SECONDS:-180}"

if [[ "${1:-}" == "--foreground" ]]; then
    FOREGROUND=true
fi

if [[ ! -f "$LOCAL_ENV_FILE" ]]; then
    echo "Falta la configuración local privada: src/backend/.env.localhost"
    echo "Cree el archivo a partir de src/backend/.env.localhost.example."
    exit 1
fi

set -a
# Este archivo está ignorado por Git y solo contiene credenciales ficticias locales.
source "$LOCAL_ENV_FILE"
set +a

if [[ -z "${LOCAL_ADMIN_PASSWORD:-}" || -z "${AUTH_TOKEN_SECRET:-}" ]]; then
    echo "LOCAL_ADMIN_PASSWORD y AUTH_TOKEN_SECRET son obligatorias en .env.localhost."
    exit 1
fi

mkdir -p "$LOCAL_DATA_DIR"

if lsof -nP -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1; then
    echo "No se inició: el puerto 8080 ya está ocupado."
    echo "Ejecute scripts/detener-local.sh o cierre el proceso que usa ese puerto."
    exit 1
fi

if lsof -nP -iTCP:5500 -sTCP:LISTEN >/dev/null 2>&1; then
    echo "No se inició: el puerto 5500 ya está ocupado."
    echo "Ejecute scripts/detener-local.sh o cierre el proceso que usa ese puerto."
    exit 1
fi

(
    cd "$BACKEND_DIR"
    nohup ./mvnw -o spring-boot:run -Dspring-boot.run.profiles=local >"$BACKEND_LOG" 2>&1 &
    echo $! >"$BACKEND_PID_FILE"
)

(
    cd "$FRONTEND_DIR"
    nohup python3 -m http.server 5500 --bind 127.0.0.1 >"$FRONTEND_LOG" 2>&1 &
    echo $! >"$FRONTEND_PID_FILE"
)

for intento in $(seq 1 "$STARTUP_TIMEOUT_SECONDS"); do
    if curl --silent --output /dev/null http://127.0.0.1:8080/api/productos 2>/dev/null; then
        echo "EMPLANORTE LOCAL está listo."
        echo "URL:        http://localhost:5500"
        echo "Usuario:    admin.local@emplanorte.test"
        echo "Contraseña: configurada en src/backend/.env.localhost (archivo ignorado)"
        echo "Base:       H2 local aislada en src/backend/.local-data"
        if [[ "$FOREGROUND" == "true" ]]; then
            trap '"$PROJECT_ROOT/scripts/detener-local.sh" >/dev/null 2>&1 || true' EXIT INT TERM
            while kill -0 "$(cat "$BACKEND_PID_FILE")" 2>/dev/null \
                    && kill -0 "$(cat "$FRONTEND_PID_FILE")" 2>/dev/null; do
                sleep 2
            done
        fi
        exit 0
    fi

    if ! kill -0 "$(cat "$BACKEND_PID_FILE")" 2>/dev/null; then
        echo "El backend local no pudo iniciar. Últimas líneas del registro:"
        tail -n 40 "$BACKEND_LOG"
        "$PROJECT_ROOT/scripts/detener-local.sh" >/dev/null 2>&1 || true
        exit 1
    fi
    sleep 1
done

echo "El backend tardó más de $STARTUP_TIMEOUT_SECONDS segundos. Revise $BACKEND_LOG"
"$PROJECT_ROOT/scripts/detener-local.sh" >/dev/null 2>&1 || true
exit 1
