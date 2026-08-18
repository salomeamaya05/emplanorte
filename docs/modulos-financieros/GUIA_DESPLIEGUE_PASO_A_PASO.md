# Guía de instalación y despliegue — EMPLANORTE 2.0

Esta guía incorpora los módulos nuevos al **mismo GitHub, mismo Render y mismo Supabase**. No se debe reemplazar ni borrar la carpeta `.git` del proyecto original.

## 1. Confirmar la rama segura

En Terminal:

```bash
cd "/Users/salomeamayarios/Desktop/SISTEMAS/septimo_semestre/SOFTII/Emplanorte-Deploy-2"
git branch --show-current
git status -sb
```

Debe aparecer:

```text
feature/proveedores-compras-facturas
```

## 2. Aplicar la entrega al proyecto actual

La forma recomendada es aplicar el archivo `emplanorte-modulos-financieros.patch` entregado junto con el ZIP.

Copie el parche a la carpeta `SOFTII` y ejecute:

```bash
cd "/Users/salomeamayarios/Desktop/SISTEMAS/septimo_semestre/SOFTII/Emplanorte-Deploy-2"
git apply --check ../emplanorte-modulos-financieros.patch
git apply ../emplanorte-modulos-financieros.patch
```

El primer comando no debe mostrar errores. Después revise:

```bash
git status --short
git diff --check
```

No use el ZIP para reemplazar la carpeta completa del proyecto; el ZIP es una copia integral de referencia y respaldo.

## 3. Validar el proyecto antes de tocar Supabase

```bash
bash scripts/validar-proyecto.sh
```

Luego compile el backend:

```bash
cd src/backend
./mvnw -DskipTests clean package
```

Debe finalizar con `BUILD SUCCESS`.

## 4. Actualizar la base de datos REAL de Supabase

Entre al proyecto existente de Supabase y abra **SQL Editor**.

### 4.1 Prevalidación obligatoria

Abra y copie todo el contenido de:

```text
src/database/00_PREVALIDAR_SUPABASE.sql
```

Ejecútelo. La primera consulta debe devolver **cero filas**. Si aparece una fila con `FALTA EN SUPABASE`, deténgase: no ejecute la migración principal.

### 4.2 Migración principal

Copie y ejecute:

```text
src/database/01_MIGRACION_PROVEEDORES_COMPRAS_FACTURAS.sql
```

El script está dentro de una transacción y agrega tablas nuevas. No elimina ventas, productos, clientes, gastos ni datos existentes.

### 4.3 Verificación

Copie y ejecute:

```text
src/database/02_VERIFICAR_MIGRACION.sql
```

Debe mostrar estas siete tablas:

```text
auditoria_compras
auditoria_pagos_proveedores
compras
detalle_compras
facturas_proveedores
pagos_proveedores
proveedores
```

También debe aparecer la función:

```text
fn_generar_numero_compra
```

### 4.4 Bucket privado de facturas

Copie y ejecute:

```text
src/database/03_CONFIGURAR_STORAGE_FACTURAS.sql
```

Debe devolver una fila para el bucket privado `facturas-proveedores`.

## 5. Configurar el backend existente en Render

Abra en Render el servicio backend actual `emplanorte-2` / `emplanorte-2-cx20` y entre a **Environment**.

Conserve las variables existentes de la base de datos y agregue:

```text
SUPABASE_URL=https://SU-PROJECT-REF.supabase.co
SUPABASE_SECRET_KEY=su_clave_secreta_del_backend
SUPABASE_STORAGE_BUCKET=facturas-proveedores
CORS_ALLOWED_ORIGINS=https://emplanorte-2-front.onrender.com
```

Puede utilizar `SUPABASE_SERVICE_ROLE_KEY` como alternativa legacy en lugar de `SUPABASE_SECRET_KEY`.

La clave secreta se coloca únicamente en el backend de Render. Nunca se pega en `api.js`, HTML, GitHub ni frontend.

Seleccione la opción de guardar y desplegar/reconstruir el servicio.

## 6. Prueba local conectada al Supabase existente

Después de aplicar la migración, puede probar el código nuevo localmente mientras la página pública sigue usando `main`.

### 6.1 Archivo local de variables

```bash
cd "/Users/salomeamayarios/Desktop/SISTEMAS/septimo_semestre/SOFTII/Emplanorte-Deploy-2/src/backend"
cp .env.example .env.local
```

Abra `.env.local` y coloque las credenciales actuales. Este archivo está ignorado por Git.

### 6.2 Levantar backend

```bash
set -a
source .env.local
set +a
./mvnw spring-boot:run
```

Espere `Started EmplanorteApplication`.

### 6.3 Levantar frontend

En otra terminal:

```bash
cd "/Users/salomeamayarios/Desktop/SISTEMAS/septimo_semestre/SOFTII/Emplanorte-Deploy-2/src/frontend"
python3 -m http.server 5500
```

Abra:

```text
http://localhost:5500
```

Ejecute el checklist incluido antes de publicar.

## 7. Guardar la rama en GitHub

Desde la raíz del proyecto:

```bash
git status --short
git diff --check
git add .
git commit -m "Implementar proveedores compras facturas pagos y balance financiero"
git push origin feature/proveedores-compras-facturas
```

La versión pública todavía no cambia porque Render continúa conectado a `main`.

## 8. Publicar en los mismos servicios de Render

Cuando todas las pruebas estén aprobadas:

```bash
git switch main
git pull origin main
git merge --no-ff feature/proveedores-compras-facturas -m "Integrar módulos financieros de EMPLANORTE"
git push origin main
```

Render detectará el cambio en la rama enlazada y desplegará el backend y el frontend existentes.

Revise primero que el backend quede `Live`; después verifique el frontend y recargue con `Command + Shift + R`.

## 9. Verificación posterior a publicación

Compruebe en este orden:

1. Inicio de sesión.
2. Ventas existentes.
3. Inventario existente.
4. Gastos existentes.
5. Proveedores.
6. Compra de prueba.
7. Factura y abono.
8. Dashboard.
9. Soporte PDF/imagen.

## 10. Regreso seguro del código

La migración es aditiva: si se revierte el código, las tablas nuevas pueden permanecer sin afectar los módulos antiguos.

Para revertir el merge, localice el commit de integración:

```bash
git log --oneline --merges -5
```

Después:

```bash
git revert -m 1 ID_DEL_COMMIT_DE_MERGE
git push origin main
```

No borre tablas de Supabase durante una emergencia. Primero restaure el código y conserve la información registrada.
