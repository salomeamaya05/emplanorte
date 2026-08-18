# Validación técnica de la entrega

## Verificaciones realizadas en la preparación

- Sintaxis Java de todos los archivos nuevos y modificados, utilizando compilación estática con stubs de Spring/JPA/Lombok.
- Sintaxis JavaScript de `api.js`, `utils.js` y scripts embebidos en las páginas nuevas mediante `node --check`.
- Revisión de IDs HTML duplicados en páginas nuevas.
- Revisión de rutas API frontend/backend.
- Revisión del esquema SQL aditivo, llaves foráneas, restricciones, índices, secuencia y función de consecutivo.
- Limpieza de archivos `.DS_Store`, temporales y secretos.
- Protección de contraseña hash en respuestas JSON.
- Compatibilidad con clave nueva `SUPABASE_SECRET_KEY` y clave legacy `SUPABASE_SERVICE_ROLE_KEY`.

## Límite de la validación

La base de datos real de Supabase no fue modificada desde este entorno y no se incluyeron credenciales. Por esa razón, la validación final depende de ejecutar primero `00_PREVALIDAR_SUPABASE.sql` sobre el proyecto real.

El empaquetado completo de Maven no pudo descargarse en el entorno de preparación sin acceso al repositorio Maven. Debe ejecutarse en el Mac con:

```bash
cd src/backend
./mvnw -DskipTests clean package
```

No debe fusionarse con `main` si ese comando no termina en `BUILD SUCCESS` o si alguna prueba del checklist falla.
