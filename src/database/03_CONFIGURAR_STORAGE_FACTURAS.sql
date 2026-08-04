-- ============================================================
-- CONFIGURACIÓN DEL BUCKET PRIVADO PARA SOPORTES DE FACTURAS
-- Ejecútelo DESPUÉS de la migración principal.
-- ============================================================

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'facturas-proveedores',
    'facturas-proveedores',
    FALSE,
    10485760,
    ARRAY['application/pdf','image/jpeg','image/png','image/webp']
)
ON CONFLICT (id) DO UPDATE
SET public = FALSE,
    file_size_limit = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

SELECT id, name, public, file_size_limit, allowed_mime_types
FROM storage.buckets
WHERE id='facturas-proveedores';
