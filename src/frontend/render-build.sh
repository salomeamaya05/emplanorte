#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${API_BASE_URL:-}" ]]; then
    echo "Falta API_BASE_URL. El despliegue se detiene para no conectar staging con producción."
    exit 1
fi

if [[ ! "$API_BASE_URL" =~ ^https://[A-Za-z0-9.-]+/api/?$ ]]; then
    echo "API_BASE_URL debe tener el formato https://servicio.onrender.com/api"
    exit 1
fi

NORMALIZED_API_BASE_URL="${API_BASE_URL%/}"
printf "window.EMPLANORTE_API_BASE_URL = '%s';\n" "$NORMALIZED_API_BASE_URL" > js/runtime-config.js
echo "Configuración pública del frontend generada correctamente."
