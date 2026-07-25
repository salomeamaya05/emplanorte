# EMPLANORTE: despliegue con Supabase + Render

## Arquitectura

- Frontend: Render Static Site
- Backend: Render Web Service (Docker)
- Base de datos: Supabase PostgreSQL
- Repositorio: GitHub `salomeamaya05/emplanorte`

## URLs actuales

- Frontend: `https://emplanorte-2-front.onrender.com`
- Backend: `https://emplanorte-2-cx20.onrender.com`
- Prueba del backend: `https://emplanorte-2-cx20.onrender.com/api/productos`

## Variables del backend en Render

Configure estas variables en el Web Service `emplanorte-2`:

- `DATABASE_URL`: URL JDBC de Supabase. Debe empezar por `jdbc:postgresql://` y terminar con `?sslmode=require`.
- `DB_USERNAME`: usuario de conexión de Supabase.
- `DB_PASSWORD`: contraseña de la base de datos de Supabase.
- `CORS_ALLOWED_ORIGINS`: `https://emplanorte-2-front.onrender.com`

No guarde contraseñas en GitHub.

## Conexión recomendada de Supabase

En Supabase abra **Connect** y use el **Session pooler** si la conexión directa IPv6 no funciona desde Render. Convierta la cadena a JDBC:

`postgresql://USUARIO:CLAVE@HOST:5432/postgres?sslmode=require`

se convierte en:

`jdbc:postgresql://HOST:5432/postgres?sslmode=require`

El usuario y la contraseña se colocan por separado en `DB_USERNAME` y `DB_PASSWORD`.

## Esquema inicial

El backend usa `spring.jpa.hibernate.ddl-auto=none`, por lo que las tablas deben existir. Ejecute una sola vez el archivo:

`src/database/emplanorte_db.sql`

en el SQL Editor de Supabase.

## Render gratuito

El Web Service gratuito puede dormirse después de un periodo sin uso y tardar al reactivarse. El Static Site permanece publicado. No use Render Postgres gratuito para los datos porque expira a los 30 días.
