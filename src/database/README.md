# Base de Datos - EMPLANORTE S.A.S.

Diseño e implementación de la base de datos relacional para el sistema, estructurada en el motor **PostgreSQL**.

## 📂 Organización de Scripts SQL
Para mayor orden y facilidad en el mantenimiento, la base de datos se separa en scripts lógicos independientes:

1. `01_schema.sql` — Creación de tablas, llaves primarias, llaves foráneas y restricciones `CHECK`.
2. `02_indexes.sql` — Índices de optimización (`CREATE INDEX`) para mejorar los tiempos de consulta.
3. `03_views.sql` — Vistas precompiladas para reportes rápidos y el panel principal.
4. `04_functions_triggers.sql` — Funciones pl/pgsql y disparadores automáticos (triggers) para lógica interna (por ejemplo, descuento automático de inventario tras una venta).
5. `05_seed_data.sql` — Datos iniciales de prueba (productos base, categorías predefinidas, clientes y usuarios de prueba) para inicializar el entorno.

## 🚀 Instalación en PostgreSQL
Ejecutar los archivos en orden ascendente usando tu cliente preferido (pgAdmin, DBeaver, terminal psql, etc.).

## Actualización: flete distribuido por pacas

En una base de datos que ya contiene compras, ejecutar una sola vez:

`06_MIGRACION_FLETE_POR_PACAS.sql`

La migración es aditiva: conserva productos y compras anteriores, marca sus cálculos como históricos y agrega la presentación por paca para las compras nuevas.
