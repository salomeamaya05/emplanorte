# Guía de Despliegue en Render — EMPLANORTE S.A.S.

- **Base de datos:** PostgreSQL en **Neon** (ya desplegada).
- **Backend:** Spring Boot en **Render** (Web Service con Docker).
- **Frontend:** estático en **Render** (Static Site).

---

## 1. Preparar la base de datos en Neon (UNA sola vez)

El backend usa `ddl-auto=none`: **no crea las tablas solo**. Carga el esquema en Neon:

1. En Neon copia tu connection string (formato
   `postgresql://USER:PASSWORD@HOST/DBNAME?sslmode=require`).
2. Ejecuta el script contra Neon (necesitas `psql`):
   ```bash
   psql "postgresql://USER:PASSWORD@HOST/DBNAME?sslmode=require" -f src/database/emplanorte_db.sql
   ```
   También puedes pegar el contenido de `src/database/emplanorte_db.sql` en el **SQL Editor** de Neon.
3. Esto crea tablas, vistas, triggers y los usuarios de prueba.

## 2. Subir el código a GitHub

```bash
git add -A
git commit -m "Configuración de despliegue en Render"
git push origin main
```

## 3. Crear el BACKEND en Render

1. Render → **New +** → **Web Service** → conecta el repo.
2. Configuración:
   - **Root Directory:** `src/backend`
   - **Runtime / Language:** Docker (detecta el `Dockerfile` automáticamente)
   - **Name:** `emplanorte-backend` (si lo cambias, ajusta `api.js`, ver paso 5)
3. En **Environment** agrega las variables del bloque de abajo.
4. Crea el servicio y espera el build (varios minutos la primera vez).

## 4. Crear el FRONTEND en Render

1. Render → **New +** → **Static Site** → mismo repo.
2. Configuración:
   - **Root Directory:** `src/frontend`
   - **Build Command:** *(vacío)*
   - **Publish Directory:** `.`
   - **Name:** `emplanorte-frontend`

## 5. Verificar URLs

- Backend:  `https://emplanorte-backend.onrender.com`
- Frontend: `https://emplanorte-frontend.onrender.com`

Si Render asignó nombres/URLs distintos:
- Actualiza `API_BASE_URL` en `src/frontend/js/api.js` con la URL real del backend + `/api`.
- Actualiza la variable `CORS_ALLOWED_ORIGINS` del backend con la URL real del frontend.

## 6. Probar

Abre el frontend e inicia sesión:
- **Correo:** `duvan@emplanorte.com`
- **Contraseña:** `Admin2024*`

---

## Variables de entorno en Render (servicio backend)

| Variable | Valor | Notas |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://HOST/DBNAME?sslmode=require` | Toma HOST y DBNAME de tu string de Neon. **Sin** usuario/clave aquí y **con** `?sslmode=require`. Si Neon te da el host con `-pooler`, úsalo tal cual. |
| `DB_USER` | usuario de Neon | Ej: `neondb_owner` |
| `DB_PASSWORD` | contraseña de Neon | |
| `CORS_ALLOWED_ORIGINS` | `https://emplanorte-frontend.onrender.com` | URL real de tu frontend en Render |

> `PORT` lo define Render automáticamente; no lo agregues (la app ya lee `${PORT}`).

### Ejemplo de DB_URL a partir del string de Neon

Si Neon te da:
```
postgresql://neondb_owner:abc123@ep-cool-name-12345.us-east-2.aws.neon.tech/neondb?sslmode=require
```
Entonces:
- `DB_URL` = `jdbc:postgresql://ep-cool-name-12345.us-east-2.aws.neon.tech/neondb?sslmode=require`
- `DB_USER` = `neondb_owner`
- `DB_PASSWORD` = `abc123`

---

## Notas

- **Plan Free de Render:** el backend se duerme tras ~15 min inactivo; la primera petición
  luego tarda ~30–50 s.
- **Seguridad:** la API está abierta (`anyRequest().permitAll()`) y el token Bearer no se
  valida en el servidor. Apto para demo/entrega, no para datos reales sin JWT.
