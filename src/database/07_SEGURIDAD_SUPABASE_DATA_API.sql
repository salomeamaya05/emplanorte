-- Seguridad del Data API de Supabase para EMPLANORTE.
--
-- Arquitectura actual:
--   frontend -> backend Spring Boot -> PostgreSQL (rol postgres vía pooler)
--
-- El frontend no consulta las tablas de negocio mediante PostgREST. Por eso
-- anon y authenticated no deben tener acceso directo a public. El rol postgres
-- del backend conserva sus permisos y no se usa FORCE ROW LEVEL SECURITY.

-- Cerrar el acceso directo existente a tablas, vistas y secuencias.
REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM anon, authenticated;
REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM anon, authenticated;

-- Las funciones de public no son endpoints de la aplicación. Los triggers
-- continúan ejecutándose internamente y postgres conserva EXECUTE.
REVOKE EXECUTE ON ALL FUNCTIONS IN SCHEMA public FROM PUBLIC, anon, authenticated;

-- Evitar que objetos nuevos creados por postgres queden expuestos por defecto.
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    REVOKE ALL PRIVILEGES ON TABLES FROM anon, authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    REVOKE ALL PRIVILEGES ON SEQUENCES FROM anon, authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC, anon, authenticated;

-- Las vistas deben usar los permisos del invocador y respetar RLS en sus tablas.
ALTER VIEW public.v_inventario SET (security_invoker = true);
ALTER VIEW public.v_ventas SET (security_invoker = true);
ALTER VIEW public.v_resumen_financiero_diario SET (security_invoker = true);

-- Fijar un search_path vacío elimina sustituciones de objetos por search_path.
-- Todas las relaciones usadas por estas funciones se califican con public.
CREATE OR REPLACE FUNCTION public.fn_actualizar_timestamp()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = ''
AS $$
BEGIN
    NEW.actualizado_en = pg_catalog.now();
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_generar_numero_compra()
RETURNS text
LANGUAGE plpgsql
SET search_path = ''
AS $$
BEGIN
    RETURN 'CMP-' || pg_catalog.lpad(
        pg_catalog.nextval('public.seq_numero_compra'::pg_catalog.regclass)::text,
        6,
        '0'
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_generar_numero_cotizacion()
RETURNS text
LANGUAGE plpgsql
SET search_path = ''
AS $$
DECLARE
    v_ultimo integer;
BEGIN
    SELECT COALESCE(MAX(CAST(SUBSTRING(c.numero_cotizacion FROM 5) AS integer)), 0)
      INTO v_ultimo
      FROM public.cotizaciones c;

    RETURN 'COT-' || pg_catalog.lpad((v_ultimo + 1)::text, 6, '0');
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_generar_numero_venta()
RETURNS varchar
LANGUAGE plpgsql
SET search_path = ''
AS $$
DECLARE
    siguiente_numero integer;
BEGIN
    SELECT COALESCE(
        MAX(
            CASE
                WHEN v.numero_venta ~ '^EMPLANORTE-[0-9]+$'
                THEN SUBSTRING(v.numero_venta FROM '([0-9]+)$')::integer
                ELSE 0
            END
        ),
        0
    ) + 1
      INTO siguiente_numero
      FROM public.ventas v;

    RETURN 'EMPLANORTE-' || pg_catalog.lpad(siguiente_numero::text, 3, '0');
END;
$$;

-- Mantener explícitos los permisos requeridos por la conexión JDBC del backend.
GRANT EXECUTE ON FUNCTION public.fn_actualizar_timestamp() TO postgres;
GRANT EXECUTE ON FUNCTION public.fn_generar_numero_compra() TO postgres;
GRANT EXECUTE ON FUNCTION public.fn_generar_numero_cotizacion() TO postgres;
GRANT EXECUTE ON FUNCTION public.fn_generar_numero_venta() TO postgres;
