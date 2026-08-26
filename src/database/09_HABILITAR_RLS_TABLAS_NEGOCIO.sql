-- Defensa en profundidad para las tablas de negocio de EMPLANORTE.
--
-- El Data API ya está cerrado para anon/authenticated mediante la migración 07.
-- Estas tablas no necesitan políticas públicas porque el único acceso de la
-- aplicación es el backend JDBC con postgres. No se usa FORCE RLS.

ALTER TABLE public.usuarios ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categorias_producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.productos ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.clientes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ventas ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.detalle_ventas ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cotizaciones ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.detalle_cotizaciones ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.proveedores ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.compras ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.detalle_compras ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.facturas_proveedores ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pagos_proveedores ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categorias_gasto ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.gastos ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.auditoria_ventas ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.auditoria_compras ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.auditoria_gastos ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.auditoria_pagos_proveedores ENABLE ROW LEVEL SECURITY;

-- Las dos tablas nuevas ya nacen con RLS en la migración 08. Estas sentencias
-- se mantienen idempotentes para instalaciones nuevas o recuperaciones.
ALTER TABLE public.creditos_venta ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.abonos_credito ENABLE ROW LEVEL SECURITY;
