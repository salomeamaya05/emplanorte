-- ============================================================
--  SCRIPT DE BASE DE DATOS UNIFICADO - EMPLANORTE S.A.S.
--  Sistema Web de Gestión Comercial, Inventario y Reportes
--  Motor: PostgreSQL
--  Versión: 2.0 (Optimizado para 14 Requisitos Funcionales)
--  Descripción: Estructura limpia (10 tablas), restricciones,
--               índices, vistas dinámicas, triggers y datos de prueba.
-- ============================================================




SET client_encoding = 'UTF8';

-- ============================================================
--  EXTENSIONES (Requeridas para generación de Hash)
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";


-- ============================================================
--  1. TABLAS PRINCIPALES
-- ============================================================

-- TABLA: usuarios (RNF05 - Control de acceso con autenticación)
CREATE TABLE usuarios (
    id              SERIAL          PRIMARY KEY,
    nombre          VARCHAR(100)    NOT NULL,
    correo          VARCHAR(150)    NOT NULL UNIQUE,
    contrasena_hash TEXT            NOT NULL,
    rol             VARCHAR(30)     NOT NULL DEFAULT 'administrador'
                                    CHECK (rol IN ('administrador', 'superadmin')),
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE usuarios IS 'Usuarios autorizados para acceder al sistema';

-- TABLA: categorias_producto (RF03 - Categorización de productos)
CREATE TABLE categorias_producto (
    id              SERIAL          PRIMARY KEY,
    nombre          VARCHAR(100)    NOT NULL UNIQUE,
    descripcion     TEXT,
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE categorias_producto IS 'Categorías de los productos en inventario (PET, Ámbar, Aseo, etc.)';

-- TABLA: productos (RF01, RF02, RF03, RF04 - Gestión de inventario)
CREATE TABLE productos (
    id                  SERIAL          PRIMARY KEY,
    codigo              VARCHAR(50)     NOT NULL UNIQUE,
    nombre              VARCHAR(150)    NOT NULL,
    descripcion         TEXT,
    id_categoria        INT             NOT NULL REFERENCES categorias_producto(id),
    capacidad_ml        DECIMAL(10,2),
    costo_unitario      DECIMAL(12,2)   NOT NULL CHECK (costo_unitario >= 0),
    precio_venta        DECIMAL(12,2)   NOT NULL CHECK (precio_venta >= 0),
    stock_disponible    INT             NOT NULL DEFAULT 0 CHECK (stock_disponible >= 0),
    stock_minimo        INT             NOT NULL DEFAULT 5 CHECK (stock_minimo >= 0),
    unidad_medida       VARCHAR(30)     NOT NULL DEFAULT 'unidad',
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMP       NOT NULL DEFAULT NOW(),
    actualizado_en      TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE productos IS 'Catálogo e inventario de productos de EMPLANORTE';

-- TABLA: clientes (RF13 - Registro de clientes)
CREATE TABLE clientes (
    id              SERIAL          PRIMARY KEY,
    nombre          VARCHAR(150)    NOT NULL,
    telefono        VARCHAR(20)     NOT NULL,
    direccion       TEXT,
    observaciones   TEXT,
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP       NOT NULL DEFAULT NOW(),
    actualizado_en  TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE clientes IS 'Clientes registrados del negocio para historial comercial';

-- TABLA: ventas (RF05, RF07 - Gestión de ventas)
CREATE TABLE ventas (
    id                  SERIAL          PRIMARY KEY,
    numero_venta        VARCHAR(20)     NOT NULL UNIQUE,
    id_cliente          INT             REFERENCES clientes(id),
    id_usuario          INT             NOT NULL REFERENCES usuarios(id),
    fecha_venta         TIMESTAMP       NOT NULL DEFAULT NOW(),
    subtotal            DECIMAL(14,2)   NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    descuento           DECIMAL(14,2)   NOT NULL DEFAULT 0 CHECK (descuento >= 0),
    total               DECIMAL(14,2)   NOT NULL DEFAULT 0 CHECK (total >= 0),
    total_costo         DECIMAL(14,2)   NOT NULL DEFAULT 0 CHECK (total_costo >= 0),
    ganancia            DECIMAL(14,2)   NOT NULL DEFAULT 0,
    metodo_pago         VARCHAR(30)     NOT NULL CHECK (metodo_pago IN ('efectivo', 'transferencia', 'tarjeta', 'credito', 'otro')),
    estado              VARCHAR(20)     NOT NULL DEFAULT 'completada' CHECK (estado IN ('completada', 'anulada')),
    observaciones       TEXT,
    editada             BOOLEAN         NOT NULL DEFAULT FALSE,
    fecha_ultima_edicion TIMESTAMP,
    id_venta_origen     INT             REFERENCES ventas(id) ON DELETE SET NULL,
    id_venta_reemplazo  INT             REFERENCES ventas(id) ON DELETE SET NULL,
    motivo_anulacion    TEXT,
    creado_en           TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE ventas IS 'Registro maestro de transacciones de venta';

-- TABLA: detalle_ventas (RF06, RF07, RF08 - Líneas de detalle de venta)
CREATE TABLE detalle_ventas (
    id                  SERIAL          PRIMARY KEY,
    id_venta            INT             NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    id_producto         INT             NOT NULL REFERENCES productos(id),
    cantidad            INT             NOT NULL CHECK (cantidad > 0),
    precio_unitario     DECIMAL(12,2)   NOT NULL CHECK (precio_unitario >= 0),
    costo_unitario      DECIMAL(12,2)   NOT NULL CHECK (costo_unitario >= 0),
    subtotal_linea      DECIMAL(14,2)   NOT NULL CHECK (subtotal_linea >= 0),
    costo_linea         DECIMAL(14,2)   NOT NULL CHECK (costo_linea >= 0),
    ganancia_linea      DECIMAL(14,2)   NOT NULL
);

COMMENT ON TABLE detalle_ventas IS 'Detalle de los productos incluidos en cada venta';

-- TABLA: auditoria_ventas (Bitácora de creación/anulación de ventas)
-- Las ventas son inmutables: se anulan (devolviendo el stock) y se crea una nueva.
CREATE TABLE auditoria_ventas (
    id             SERIAL          PRIMARY KEY,
    id_venta       INT             NOT NULL REFERENCES ventas(id),
    id_usuario     INT             REFERENCES usuarios(id),
    usuario_nombre VARCHAR(150),
    accion         VARCHAR(30)     NOT NULL CHECK (accion IN ('creacion', 'edicion', 'anulacion', 'anulacion_correccion', 'correccion_creada')),
    numero_venta   VARCHAR(20),
    total          DECIMAL(14,2),
    estado         VARCHAR(20),
    motivo         TEXT,
    detalle_cambios TEXT,
    id_venta_relacionada INT       REFERENCES ventas(id) ON DELETE SET NULL,
    fecha_registro TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE auditoria_ventas IS 'Bitácora de auditoría: creación y anulación de ventas';

-- TABLA: categorias_gasto (RF10 - Clasificación de gastos)
CREATE TABLE categorias_gasto (
    id              SERIAL          PRIMARY KEY,
    nombre          VARCHAR(100)    NOT NULL UNIQUE,
    descripcion     TEXT,
    activo          BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE categorias_gasto IS 'Categorías para organizar los egresos del negocio';

-- TABLA: gastos (RF09 - Registro de gastos)
CREATE TABLE gastos (
    id              SERIAL          PRIMARY KEY,
    id_categoria    INT             NOT NULL REFERENCES categorias_gasto(id),
    id_usuario      INT             NOT NULL REFERENCES usuarios(id),
    descripcion     VARCHAR(250)    NOT NULL,
    valor           DECIMAL(14,2)   NOT NULL CHECK (valor > 0),
    fecha_gasto     DATE            NOT NULL DEFAULT CURRENT_DATE,
    creado_en       TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE gastos IS 'Registro de egresos operativos y de funcionamiento';

-- TABLA: auditoria_gastos (Bitácora de cambios de gastos)
-- Guarda un snapshot inmutable por cada creación/edición de un gasto: quién y cuándo.
CREATE TABLE auditoria_gastos (
    id               SERIAL          PRIMARY KEY,
    id_gasto         INT             NOT NULL REFERENCES gastos(id),
    id_usuario       INT             REFERENCES usuarios(id),
    usuario_nombre   VARCHAR(150),
    accion           VARCHAR(20)     NOT NULL CHECK (accion IN ('creacion', 'edicion')),
    categoria_nombre VARCHAR(100),
    descripcion      VARCHAR(250),
    valor            DECIMAL(14,2),
    fecha_gasto      DATE,
    fecha_registro   TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE auditoria_gastos IS 'Bitácora de auditoría: historial de cambios de cada gasto';

-- TABLA: cotizaciones (RF14 - Registro de cotizaciones)
CREATE TABLE cotizaciones (
    id                  SERIAL          PRIMARY KEY,
    numero_cotizacion   VARCHAR(20)     NOT NULL UNIQUE,
    id_cliente          INT             NOT NULL REFERENCES clientes(id),
    id_usuario          INT             NOT NULL REFERENCES usuarios(id),
    fecha_cotizacion    TIMESTAMP       NOT NULL DEFAULT NOW(),
    subtotal            DECIMAL(14,2)   NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    descuento           DECIMAL(14,2)   NOT NULL DEFAULT 0 CHECK (descuento >= 0),
    total               DECIMAL(14,2)   NOT NULL DEFAULT 0 CHECK (total >= 0),
    estado              VARCHAR(20)     NOT NULL DEFAULT 'pendiente' 
                                        CHECK (estado IN ('pendiente', 'aceptada', 'rechazada', 'convertida')),
    notas               TEXT,
    creado_en           TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE cotizaciones IS 'Propuestas comerciales generadas para los clientes';

-- TABLA: detalle_cotizaciones (RF14 - Detalles de cotización)
CREATE TABLE detalle_cotizaciones (
    id                  SERIAL          PRIMARY KEY,
    id_cotizacion       INT             NOT NULL REFERENCES cotizaciones(id) ON DELETE CASCADE,
    id_producto         INT             NOT NULL REFERENCES productos(id),
    cantidad            INT             NOT NULL CHECK (cantidad > 0),
    precio_unitario     DECIMAL(12,2)   NOT NULL CHECK (precio_unitario >= 0),
    subtotal_linea      DECIMAL(14,2)   NOT NULL CHECK (subtotal_linea >= 0)
);

COMMENT ON TABLE detalle_cotizaciones IS 'Detalle de los productos incluidos en cada cotización';


-- ============================================================
--  2. ÍNDICES DE RENDIMIENTO (RNF04)
-- ============================================================
CREATE INDEX idx_productos_busqueda     ON productos(activo, id_categoria);
CREATE INDEX idx_productos_nombre       ON productos(nombre);
CREATE INDEX idx_ventas_fecha           ON ventas(fecha_venta);
CREATE INDEX idx_ventas_cliente         ON ventas(id_cliente);
CREATE UNIQUE INDEX uq_ventas_id_venta_origen ON ventas(id_venta_origen) WHERE id_venta_origen IS NOT NULL;
CREATE UNIQUE INDEX uq_ventas_id_venta_reemplazo ON ventas(id_venta_reemplazo) WHERE id_venta_reemplazo IS NOT NULL;
CREATE INDEX idx_auditoria_ventas_fecha ON auditoria_ventas(id_venta, fecha_registro);
CREATE INDEX idx_gastos_fecha           ON gastos(fecha_gasto);
CREATE INDEX idx_cotizaciones_fecha     ON cotizaciones(fecha_cotizacion);


-- ============================================================
--  3. VISTAS DE CONSULTA RÁPIDA (RF11, RF12, RF14)
-- ============================================================

-- Vista del Inventario con Alertas de Stock y Márgenes de Ganancia
CREATE OR REPLACE VIEW v_inventario AS
SELECT
    p.id,
    p.codigo,
    p.nombre,
    c.nombre AS categoria,
    p.capacidad_ml,
    p.costo_unitario,
    p.precio_venta,
    ROUND(p.precio_venta - p.costo_unitario, 2) AS margen_ganancia_unitario,
    p.stock_disponible,
    p.stock_minimo,
    CASE WHEN p.stock_disponible <= p.stock_minimo THEN TRUE ELSE FALSE END AS alerta_stock,
    p.unidad_medida,
    p.activo
FROM productos p
JOIN categorias_producto c ON c.id = p.id_categoria;

-- Vista Enriquecida de Ventas
CREATE OR REPLACE VIEW v_ventas AS
SELECT
    v.id,
    v.numero_venta,
    v.fecha_venta,
    COALESCE(cl.nombre, 'Cliente General') AS cliente,
    u.nombre AS vendedor,
    v.subtotal,
    v.descuento,
    v.total,
    v.total_costo,
    v.ganancia,
    v.metodo_pago,
    v.estado
FROM ventas v
JOIN usuarios u ON u.id = v.id_usuario
LEFT JOIN clientes cl ON cl.id = v.id_cliente;

-- Vista de Resumen Financiero Diario para Dashboard (RF11)
-- Calcula totales de ventas, gastos y ganancias netas directamente en base a la fecha
CREATE OR REPLACE VIEW v_resumen_financiero_diario AS
SELECT 
    fechas.fecha,
    COALESCE(SUM(v.total), 0) AS total_ventas,
    COALESCE(g.total_gastos, 0) AS total_gastos,
    COALESCE(SUM(v.ganancia), 0) AS ganancias_ventas,
    COALESCE(SUM(v.ganancia), 0) - COALESCE(g.total_gastos, 0) AS ganancia_neta_real
FROM (
    SELECT DISTINCT DATE(fecha_venta) AS fecha FROM ventas WHERE estado = 'completada'
    UNION
    SELECT DISTINCT fecha_gasto AS fecha FROM gastos
) fechas
LEFT JOIN ventas v ON DATE(v.fecha_venta) = fechas.fecha AND v.estado = 'completada'
LEFT JOIN (
    SELECT fecha_gasto, SUM(valor) AS total_gastos 
    FROM gastos 
    GROUP BY fecha_gasto
) g ON g.fecha_gasto = fechas.fecha
GROUP BY fechas.fecha, g.total_gastos
ORDER BY fechas.fecha DESC;


-- ============================================================
--  4. FUNCIONES Y TRIGGERS (RF08)
-- ============================================================

-- Función para actualizar fecha de modificación
CREATE OR REPLACE FUNCTION fn_actualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_productos_ts BEFORE UPDATE ON productos
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_clientes_ts BEFORE UPDATE ON clientes
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_timestamp();

-- Inventario (RF08): el descuento y la validación de stock se manejan en la capa
-- de aplicación (VentaService / CotizacionService), que es la ÚNICA fuente de
-- verdad del inventario. Antes existía un trigger fn_descontar_stock/
-- trg_descontar_stock que también descontaba; se eliminó para evitar lógica
-- duplicada y la dependencia del orden de escritura de Hibernate.
-- Los stocks de la semilla ya reflejan las ventas de ejemplo de abajo.

-- Generadores de Consecutivos (Automatización)
CREATE OR REPLACE FUNCTION fn_generar_numero_venta()
RETURNS TEXT AS $$
DECLARE
    v_ultimo INT;
BEGIN
    SELECT COALESCE(MAX(CAST(SUBSTRING(numero_venta FROM 5) AS INT)), 0) INTO v_ultimo FROM ventas;
    RETURN 'VTA-' || LPAD((v_ultimo + 1)::TEXT, 6, '0');
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_generar_numero_cotizacion()
RETURNS TEXT AS $$
DECLARE
    v_ultimo INT;
BEGIN
    SELECT COALESCE(MAX(CAST(SUBSTRING(numero_cotizacion FROM 5) AS INT)), 0) INTO v_ultimo FROM cotizaciones;
    RETURN 'COT-' || LPAD((v_ultimo + 1)::TEXT, 6, '0');
END;
$$ LANGUAGE plpgsql;


-- ============================================================
--  5. DATOS DE PRUEBA (SEED DATA)
-- ============================================================

-- Usuarios (Password defaultencriptado con crypt: 'Admin2024*')
INSERT INTO usuarios (nombre, correo, contrasena_hash, rol) VALUES
('Duvan Alvarado', 'duvan@emplanorte.com', crypt('Admin2024*', gen_salt('bf', 10)), 'superadmin'),
('Luis Chacón', 'luis@emplanorte.com', crypt('Admin2024*', gen_salt('bf', 10)), 'administrador'),
('Salomé Amaya', 'salome@emplanorte.com', crypt('Admin2024*', gen_salt('bf', 10)), 'administrador');

-- Categorías de Producto
INSERT INTO categorias_producto (nombre, descripcion) VALUES
('Envase PET', 'Envases transparentes y livianos para bebidas y productos líquidos'),
('Envase Ámbar', 'Envases oscuros que filtran luz UV, ideales para químicos o medicamentos'),
('Envase de Aseo', 'Botellas gruesas para detergentes, desinfectantes y jabones'),
('Envase Plástico General', 'Envases plásticos de polietileno de alta densidad'),
('Artículos para el Hogar', 'Artículos plásticos varios como baldes, organizadores y jarras');

-- Productos (RF03)
INSERT INTO productos (codigo, nombre, descripcion, id_categoria, capacidad_ml, costo_unitario, precio_venta, stock_disponible, stock_minimo, unidad_medida) VALUES
('PET-100', 'Envase PET 100ml con Tapa', 'Envase cilíndrico transparente para cosméticos', 1, 100.0, 350.00, 600.00, 150, 15, 'unidades'),
('PET-250', 'Envase PET 250ml con Tapa', 'Envase cilíndrico transparente estándar', 1, 250.0, 500.00, 850.00, 180, 20, 'unidades'),
('PET-500', 'Envase PET 500ml con Tapa', 'Envase para jugos o aguas con tapa de seguridad', 1, 500.0, 750.00, 1200.00, 100, 15, 'unidades'),
('AMB-100', 'Envase Ámbar 100ml con Gotero', 'Frasco de vidrio/plástico ámbar para aceites', 2, 100.0, 600.00, 1100.00, 100, 10, 'unidades'),
('AMB-250', 'Envase Ámbar 250ml con Atomizador', 'Botella ámbar ideal para esencias y tónicos', 2, 250.0, 900.00, 1500.00, 60, 10, 'unidades'),
('ASE-1000', 'Botella Aseo 1L con Asa', 'Envase con asa ergonómica para desinfectante', 3, 1000.0, 950.00, 1700.00, 100, 15, 'unidades'),
('HOG-005', 'Balde Plástico 5L con Manija', 'Balde reforzado para el hogar', 5, 5000.0, 3500.00, 6000.00, 30, 5, 'unidades');

-- Clientes
INSERT INTO clientes (nombre, telefono, direccion, observaciones) VALUES
('Distribuidora Norte S.A.S.', '3101234567', 'Calle 15 #8-40, Zona Industrial', 'Cliente mayorista frecuente'),
('Farmacia San Rafael', '3209876543', 'Avenida 6 #12-30, Centro', 'Compra envases ámbar y farmacéuticos'),
('Juan Esteban Orozco', '3157891234', 'Carrera 3 #22-15, Barrio Centro', 'Cliente recurrente al detal');

-- Categorías de Gasto
INSERT INTO categorias_gasto (nombre, descripcion) VALUES
('Transporte', 'Fletes, envíos y recargas de combustible'),
('Servicios Públicos', 'Pago de energía, agua, internet y telefonía'),
('Nómina', 'Pago de salarios al personal del negocio'),
('Compra de Mercancía', 'Adquisición de stock con proveedores'),
('Otros', 'Papelería, mantenimiento y varios');

-- Gastos (RF09)
INSERT INTO gastos (id_categoria, id_usuario, descripcion, valor, fecha_gasto) VALUES
(1, 1, 'Envío de pedido mayorista a Ocaña', 120000.00, '2026-03-01'),
(2, 1, 'Servicio de Internet y Telefonía mensual', 85000.00, '2026-03-05'),
(4, 2, 'Lote de envases PET de reposición', 350000.00, '2026-03-08'),
(5, 3, 'Resmas de papel e insumos de oficina', 45000.00, '2026-03-12');

-- Cotizaciones (RF14)
-- Cotización 1 (Pendiente)
INSERT INTO cotizaciones (numero_cotizacion, id_cliente, id_usuario, fecha_cotizacion, subtotal, descuento, total, estado, notas) VALUES
('COT-000001', 1, 2, '2026-03-10 10:00:00', 60000.00, 5000.00, 55000.00, 'pendiente', 'Cotización para pedido especial de botellas PET');
INSERT INTO detalle_cotizaciones (id_cotizacion, id_producto, cantidad, precio_unitario, subtotal_linea) VALUES
(1, 1, 100, 600.00, 60000.00);

-- Cotización 2 (Convertida)
INSERT INTO cotizaciones (numero_cotizacion, id_cliente, id_usuario, fecha_cotizacion, subtotal, descuento, total, estado, notas) VALUES
('COT-000002', 2, 3, '2026-03-14 11:30:00', 110000.00, 0.00, 110000.00, 'convertida', 'Envases ámbar para medicamentos');
INSERT INTO detalle_cotizaciones (id_cotizacion, id_producto, cantidad, precio_unitario, subtotal_linea) VALUES
(2, 4, 100, 1100.00, 110000.00);

-- Ventas (RF05)
-- Venta 1 (Asociada a COT-000002)
INSERT INTO ventas (numero_venta, id_cliente, id_usuario, fecha_venta, subtotal, descuento, total, total_costo, ganancia, metodo_pago, estado, observaciones) VALUES
('VTA-000001', 2, 3, '2026-03-15 14:00:00', 110000.00, 0.00, 110000.00, 60000.00, 50000.00, 'transferencia', 'completada', 'Conversión automática de la cotización COT-000002');
INSERT INTO detalle_ventas (id_venta, id_producto, cantidad, precio_unitario, costo_unitario, subtotal_linea, costo_linea, ganancia_linea) VALUES
(1, 4, 100, 1100.00, 600.00, 110000.00, 60000.00, 50000.00); -- El stock del producto 4 ya viene ajustado en su INSERT

-- Venta 2 (Directa)
INSERT INTO ventas (numero_venta, id_cliente, id_usuario, fecha_venta, subtotal, descuento, total, total_costo, ganancia, metodo_pago, estado, observaciones) VALUES
('VTA-000002', 3, 2, '2026-03-16 09:15:00', 51000.00, 2000.00, 49000.00, 31000.00, 18000.00, 'efectivo', 'completada', 'Venta al detal directa en mostrador');
INSERT INTO detalle_ventas (id_venta, id_producto, cantidad, precio_unitario, costo_unitario, subtotal_linea, costo_linea, ganancia_linea) VALUES
(2, 2, 20, 850.00, 500.00, 17000.00, 10000.00, 7000.00),
(2, 6, 20, 1700.00, 950.00, 34000.00, 19000.00, 15000.00); -- El stock de los productos 2 y 6 ya viene ajustado en sus INSERT
