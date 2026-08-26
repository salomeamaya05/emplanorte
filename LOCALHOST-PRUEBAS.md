# EMPLANORTE — entorno local aislado

Este entorno está diseñado para probar, editar y eliminar datos ficticios sin
conectarse a Supabase ni modificar producción.

## Iniciar

Desde la raíz del proyecto:

```bash
bash scripts/iniciar-local.sh
```

Espere el mensaje `EMPLANORTE LOCAL está listo` y abra:

```text
http://localhost:5500
```

Antes del primer inicio, cree el archivo privado local:

```bash
cp src/backend/.env.localhost.example src/backend/.env.localhost
```

Edite únicamente `src/backend/.env.localhost` y reemplace los marcadores. Este
archivo queda ignorado por Git y nunca debe copiar valores desde `.env.local`.

El usuario local permanente es `admin.local@emplanorte.test`. Su contraseña se
lee de `LOCAL_ADMIN_PASSWORD` y no se guarda en los archivos versionados.

## Detener

```bash
bash scripts/detener-local.sh
```

## Garantías de aislamiento

- El perfil `local` utiliza una base H2 en `src/backend/.local-data`.
- El backend escucha únicamente en `127.0.0.1:8080`.
- El frontend escucha únicamente en `127.0.0.1:5500`.
- Los soportes de facturas se guardan en la carpeta local, no en Supabase Storage.
- El arranque se detiene antes de crear el DataSource si la URL no es la base H2 local.
- `.local-data`, `.env` y `.env.local` están excluidos de Git.
- `.env.localhost` está excluido; solo se versiona su plantilla sin secretos.
- El script no carga `src/backend/.env.local`.

No ejecute el backend local haciendo `source .env.local`: ese archivo contiene la
configuración remota. Utilice siempre `scripts/iniciar-local.sh`.
